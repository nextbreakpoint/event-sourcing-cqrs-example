package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.TileGenerator;
import com.nextbreakpoint.nextfractal.core.common.TileUtils;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.WorkerExecutor;
import rx.Observable;
import rx.Single;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Objects;
import java.util.Optional;

public class TileRenderRequestedController implements Controller<TileRenderRequested, Void> {
    private final Logger logger = LoggerFactory.getLogger(TileRenderRequestedController.class.getName());

    private final Mapper<TileRenderCompleted, Message> mapper;
    private final KafkaEmitter emitter;
    private final String bucket;
    private final WorkerExecutor executor;
    private final S3AsyncClient s3AsyncClient;

    public TileRenderRequestedController(Mapper<TileRenderCompleted, Message> mapper, KafkaEmitter emitter, WorkerExecutor executor, S3AsyncClient s3AsyncClient, String bucket) {
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
        this.executor = Objects.requireNonNull(executor);
        this.s3AsyncClient = Objects.requireNonNull(s3AsyncClient);
        this.bucket = Objects.requireNonNull(bucket);
    }

    @Override
    public Single<Void> onNext(TileRenderRequested event) {
        return Single.just(event)
                .flatMap(this::onEventReceived)
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<TileRenderCompleted> onEventReceived(TileRenderRequested event) {
        return computeTile(event)
                .flatMap(result -> uploadImage(event, result))
                .map(result -> new TileRenderCompleted(event.getUuid(), event.getEvid(), event.getData(), event.getChecksum(), event.getLevel(), event.getRow(), event.getCol(), result.getError().isPresent() ? "FAILED" : "COMPLETED"));
    }

    private Single<Result> uploadImage(TileRenderRequested event, Result result) {
        final PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(createBucketKey(event)).build();
        return Observable.from(s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(result.getImage())))
                .doOnCompleted(() -> logger.info("Uploaded image " + createKey(event)))
                .map(response -> result)
                .toSingle();
    }

    private String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getUuid().toString(), event.getLevel(), event.getRow(), event.getCol());
    }

    private Single<Result> computeTile(TileRenderRequested event) {
        final GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(createBucketKey(event)).build();
        return Single.from(s3AsyncClient.getObject(request, AsyncResponseTransformer.toBytes()))
                .doOnError(err -> logger.warn("Image not found " + createBucketKey(event)))
                .map(response -> Result.of(response.asByteArray(), null))
                .onErrorResumeNext(err -> renderTile(event));
    }

    private Single<Result> renderTile(TileRenderRequested event) {
        return executor.rxExecuteBlocking(promise -> renderTileBlocking(event, promise), false);
    }

    private void renderTileBlocking(TileRenderRequested event, Promise<Result> promise) throws RuntimeException {
        try {
            final Params params = makeTileParams(event);
            final JsonObject json = new JsonObject(event.getData());
            final Bundle bundle = convertToBundle(json);
            final byte[] image = renderImage(bundle, params);
            promise.complete(Result.of(image, null));
        } catch (Exception e) {
            promise.complete(Result.of(new byte[0], e));
        }
    }

    private String createKey(TileRenderRequested event) {
        return event.getUuid() + "-" + event.getLevel() + "-" + event.getRow() + "-" + event.getCol();
    }

    private static byte[] renderImage(Bundle bundle, Params params) throws Exception {
        int side = 1 << params.getLevel();
        return TileGenerator.generateImage(TileGenerator.createTileRequest(params.getSize(), side, side, params.getRow() % side, params.getCol() % side, bundle));
    }

    private static Params makeTileParams(TileRenderRequested event) {
        final int zoom = event.getLevel();
        final int row = event.getRow();
        final int col = event.getCol();
        final int size = 256;
        return new Params(zoom, row, col, size);
    }

    private static Bundle convertToBundle(JsonObject jsonObject) throws Exception {
        final String manifest = jsonObject.getString("manifest");
        final String metadata = jsonObject.getString("metadata");
        final String script = jsonObject.getString("script");
        return TileUtils.parseData(manifest, metadata, script);
    }

    private static class Result {
        private byte[] image;
        private Throwable error;

        public static Result of(byte[] image, Throwable error) {
            return new Result(image, error);
        }

        private Result(byte[] image, Throwable error) {
            this.image = image;
            this.error = error;
        }

        public byte[] getImage() {
            return image;
        }

        public Optional<Throwable> getError() {
            return Optional.ofNullable(error);
        }
    }

    private static class Params {
        private final int level;
        private final int row;
        private final int col;
        private final int size;

        public Params(int level, int row, int col, int size) {
            this.level = level;
            this.col = col;
            this.row = row;
            this.size = size;
        }

        public int getLevel() {
            return level;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public int getSize() {
            return size;
        }
    }
}
