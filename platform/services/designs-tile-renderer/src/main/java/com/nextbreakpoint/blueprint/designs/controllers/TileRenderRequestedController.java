package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.model.Result;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.core.WorkerExecutor;
import rx.Single;

import java.util.Objects;

public class TileRenderRequestedController implements Controller<TileRenderRequested, Void> {
    private final Logger logger = LoggerFactory.getLogger(TileRenderRequestedController.class.getName());

    private final Mapper<TileRenderCompleted, Message> mapper;
    private final KafkaEmitter emitter;
    private final TileRenderer renderer;
    private final WorkerExecutor executor;
    private final S3Driver s3Driver;

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
        return s3Driver.putObject(createBucketKey(event), result.getImage())
                .doOnSuccess(response -> logger.info("Uploaded image " + createKey(event)))
                .map(response -> result);
    }

    private String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getUuid().toString(), event.getLevel(), event.getRow(), event.getCol());
    }

    private Single<Result> computeTile(TileRenderRequested event) {
        return s3Driver.getObject(createBucketKey(event))
                .doOnError(err -> logger.warn("Image not found " + createBucketKey(event)))
                .map(image -> Result.of(image, null))
                .onErrorResumeNext(err -> renderTile(event));
    }

    private Single<Result> renderTile(TileRenderRequested event) {
        return executor.rxExecuteBlocking(promise -> renderer.renderImage(event, promise), false);
    }

    private String createKey(TileRenderRequested event) {
        return event.getUuid() + "-" + event.getLevel() + "-" + event.getRow() + "-" + event.getCol();
    }
}
