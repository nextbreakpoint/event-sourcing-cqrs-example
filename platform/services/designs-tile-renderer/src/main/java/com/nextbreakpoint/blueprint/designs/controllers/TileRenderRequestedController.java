package com.nextbreakpoint.blueprint.designs.controllers;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.common.Result;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.core.WorkerExecutor;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.Objects;

import static com.nextbreakpoint.blueprint.designs.common.Bucket.createBucketKey;

public class TileRenderRequestedController implements Controller<TileRenderRequested, Void> {
    private final Logger logger = LoggerFactory.getLogger(TileRenderRequestedController.class.getName());

    private final Mapper<TileRenderCompleted, Message> mapper;
    private final KafkaEmitter emitter;
    private final WorkerExecutor executor;
    private final S3Driver s3Driver;
    private final TileRenderer renderer;

    public TileRenderRequestedController(Mapper<TileRenderCompleted, Message> mapper, KafkaEmitter emitter, WorkerExecutor executor, S3Driver s3Driver, TileRenderer renderer) {
        this.mapper = Objects.requireNonNull(mapper);
        this.emitter = Objects.requireNonNull(emitter);
        this.executor = Objects.requireNonNull(executor);
        this.s3Driver = Objects.requireNonNull(s3Driver);
        this.renderer = Objects.requireNonNull(renderer);
    }

    @Override
    public Single<Void> onNext(TileRenderRequested event) {
        return Single.just(event)
                .observeOn(Schedulers.computation())
                .flatMap(this::onTileRenderRequested)
                .map(mapper::transform)
                .flatMap(emitter::onNext);
    }

    private Single<TileRenderCompleted> onTileRenderRequested(TileRenderRequested event) {
        return renderImage(event).map(result -> createEvent(event, makeStatus(result)));
    }

    private String makeStatus(Result result) {
        return (result.getError().isPresent() || result.getImage().length == 0) ? "FAILED" : "COMPLETED";
    }

    private TileRenderCompleted createEvent(TileRenderRequested event, String status) {
        return new TileRenderCompleted(Uuids.timeBased(), event.getUuid(), event.getEsid(), event.getChecksum(), event.getLevel(), event.getRow(), event.getCol(), status);
    }

    private Single<Result> uploadImage(TileRenderRequested event, Result result) {
        return s3Driver.putObject(createBucketKey(event), result.getImage())
                .doOnSuccess(response -> logger.info("Image uploaded " + createBucketKey(event)))
                .map(response -> result);
    }

    private Single<Result> renderImage(TileRenderRequested event) {
        return s3Driver.getObject(createBucketKey(event))
                .doOnError(err -> logger.warn("Image not found " + createBucketKey(event)))
                .map(image -> Result.of(image, null))
                .doOnSuccess(result -> logger.info("Image cached " + createBucketKey(event)))
                .onErrorResumeNext(err -> renderImageBlocking(event).flatMap(result -> uploadImage(event, result)));
    }

    private Single<Result> renderImageBlocking(TileRenderRequested event) {
        return executor.rxExecuteBlocking(promise -> {
            renderer.renderImage(event, promise);
            logger.info("Image rendered " + createBucketKey(event));
        }, false);
    }
}
