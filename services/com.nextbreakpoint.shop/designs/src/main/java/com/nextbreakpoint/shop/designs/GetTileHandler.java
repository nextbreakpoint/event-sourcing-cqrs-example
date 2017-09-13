package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.nextfractal.core.Bundle;
import com.nextbreakpoint.nextfractal.core.TileGenerator;
import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static com.nextbreakpoint.shop.common.ContentType.IMAGE_PNG;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class GetTileHandler implements Handler<RoutingContext> {
    private static final int EXPIRY_TIME_IN_SECONDS = 86400;

    private final WorkerExecutor executor;

    private final Store store;

    public GetTileHandler(Store store, WorkerExecutor executor) {
        this.store = store;
        this.executor = executor;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processGetTileAsync(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processGetTileAsync(RoutingContext routingContext) {
        executor.<byte[]>rxExecuteBlocking(future -> getTileAsync(routingContext, store, future), false)
                .subscribe(result -> emitGetTileResponse(routingContext, result), err -> routingContext.fail(err));
    }

    private void getTileAsync(RoutingContext routingContext, Store store, Future<byte[]> future) {
        try {
            final TileParams params = makeTileParams(routingContext.request());

            routingContext.response().closeHandler(x -> {
                System.out.println("closed");
                Thread.currentThread().interrupt();
            });

            store.loadDesign(params.getUuid()).map(result -> result.orElseThrow(() -> Failure.notFound()))
                    .subscribe(design -> computeTile(params, future, design), err -> future.fail(err));
        } catch (Exception e) {
            future.fail(Failure.requestFailed(e));
        }
    }

    private void computeTile(TileParams params, Future<byte[]> future, JsonObject design) {
        extractJSON(design)
                .flatMap(object -> BundleUtil.parseBundle(object))
                .flatMap(bundle -> createImage(params.getX(), params.getY(), 1 << params.getZoom(), params.getSize(), bundle))
                .subscribe(bytes -> future.complete(bytes), err -> future.fail(Failure.badRequest()));
    }

    private TileParams makeTileParams(HttpServerRequest request) {
        final UUID uuid = UUID.fromString(request.getParam("uuid"));
        final int zoom = Integer.parseInt(request.getParam("zoom"));
        final int x = Integer.parseInt(request.getParam("x"));
        final int y = Integer.parseInt(request.getParam("y"));
        final int size = Integer.parseInt(request.getParam("size"));
        if (zoom < 0 || zoom > 10) {
            throw new Failure(400, "Invalid zoom level");
        }
        if (size < 128 || size > 512) {
            throw new Failure(400, "Invalid image size");
        }
        return new TileParams(uuid, zoom, x, y, size);
    }

    private Single<JsonObject> extractJSON(JsonObject result) {
        return Single.fromCallable(() -> new JsonObject(result.getString("JSON")));
    }

    private Single<byte[]> createImage(int x, int y, int side, int size, Bundle bundle) {
        return Single.fromCallable(() -> TileGenerator.generateImage(TileGenerator.createTileRequest(size, side, side,y % side,x % side, bundle)));
    }

    private void emitGetTileResponse(RoutingContext routingContext, byte[] pngImage) {
        final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        final Date now = Calendar.getInstance().getTime();
        final Date expiry = new Date(now.getTime() + EXPIRY_TIME_IN_SECONDS * 1000);
        routingContext.response()
                .putHeader(CONTENT_TYPE, IMAGE_PNG)
                .putHeader("Cache-Control", "public, max-age:" + EXPIRY_TIME_IN_SECONDS)
                .putHeader("Last-Modified", df.format(now) + " GMT")
                .putHeader("Expires", df.format(expiry) + " GMT")
                .setStatusCode(200)
                .end(new Buffer(io.vertx.core.buffer.Buffer.buffer(pngImage)));
    }

    private class TileParams {
        private final UUID uuid;
        private final int zoom;
        private final int x;
        private final int y;
        private final int size;

        public TileParams(UUID uuid, int zoom, int x, int y, int size) {
            this.uuid = uuid;
            this.zoom = zoom;
            this.x = x;
            this.y = y;
            this.size = size;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getZoom() {
            return zoom;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getSize() {
            return size;
        }
    }
}
