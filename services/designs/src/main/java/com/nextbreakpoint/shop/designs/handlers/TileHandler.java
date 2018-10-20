package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.nextfractal.core.Bundle;
import com.nextbreakpoint.nextfractal.core.TileGenerator;
import com.nextbreakpoint.nextfractal.core.TileUtils;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

import static com.nextbreakpoint.shop.common.model.ContentType.IMAGE_PNG;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;
import static rx.Single.fromCallable;

public class TileHandler implements Handler<RoutingContext> {
    private static final int EXPIRY_TIME_IN_SECONDS = 86400;

    private final WorkerExecutor executor;

    private final Store store;

    public TileHandler(Store store, WorkerExecutor executor) {
        this.store = Objects.requireNonNull(store);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        executor.<byte[]>rxExecuteBlocking(future -> getTileAsync(routingContext, store, future), false)
                .subscribe(result -> emitResponse(routingContext, result), err -> routingContext.fail(err));
    }

    private void getTileAsync(RoutingContext routingContext, Store store, Future<byte[]> future) {
        fromCallable(() -> makeTileParams(routingContext))
                .flatMap(params -> store.loadDesign(new LoadDesignRequest(params.getUuid()))
                        .map(response -> response.getDesign().orElseThrow(() -> Failure.notFound()))
                        .map(design -> new JsonObject(design.getJson()))
                        .flatMap(object -> fromCallable(() -> convertToBundle(object)))
                        .doOnEach(bundle -> routingContext.response().closeHandler(x -> Thread.currentThread().interrupt()))
                        .flatMap(bundle -> fromCallable(() -> getImage(bundle, params))))
                .subscribe(bytes -> future.complete(bytes), err -> future.fail(err));
    }

    private byte[] getImage(Bundle bundle, TileParams params) throws Exception {
        int side = 1 << params.getZoom();

        return TileGenerator.generateImage(TileGenerator.createTileRequest(params.getSize(), side, side, params.getY() % side, params.getX() % side, bundle));
    }

    private TileParams makeTileParams(RoutingContext context) {
        final HttpServerRequest request = context.request();

        final UUID uuid = UUID.fromString(request.getParam("uuid"));
        final int zoom = Integer.parseInt(request.getParam("zoom"));
        final int x = Integer.parseInt(request.getParam("x"));
        final int y = Integer.parseInt(request.getParam("y"));
        final int size = Integer.parseInt(request.getParam("size"));

        if (zoom < 0 || zoom > 10) {
            throw new Failure(400, "Invalid zoom level: " + zoom);
        }

        if (size < 128 || size > 512) {
            throw new Failure(400, "Invalid image size: " + size);
        }

        return new TileParams(uuid, zoom, x, y, size);
    }

    private static Bundle convertToBundle(JsonObject jsonObject) throws Exception {
        final String manifest = jsonObject.getString("manifest");
        final String metadata = jsonObject.getString("metadata");
        final String script = jsonObject.getString("script");

        return TileUtils.parseData(manifest, metadata, script);
    }

    private void emitResponse(RoutingContext routingContext, byte[] pngImage) {
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
