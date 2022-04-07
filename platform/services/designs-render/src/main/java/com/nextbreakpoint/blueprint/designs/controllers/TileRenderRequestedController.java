package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.common.Result;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import io.vertx.rxjava.core.WorkerExecutor;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

import static com.nextbreakpoint.blueprint.designs.common.Bucket.createBucketKey;

@Log4j2
public class TileRenderRequestedController implements Controller<InputMessage, Void> {
    private Mapper<InputMessage, TileRenderRequested> inputMapper;
    private final MessageMapper<TileRenderCompleted, OutputMessage> outputMapper;
    private final MessageEmitter emitter;
    private final WorkerExecutor executor;
    private final S3Driver s3Driver;
    private final TileRenderer renderer;

    public TileRenderRequestedController(Mapper<InputMessage, TileRenderRequested> inputMapper, MessageMapper<TileRenderCompleted, OutputMessage> outputMapper, MessageEmitter emitter, WorkerExecutor executor, S3Driver s3Driver, TileRenderer renderer) {
        this.inputMapper = Objects.requireNonNull(inputMapper);;
        this.outputMapper = Objects.requireNonNull(outputMapper);
        this.emitter = Objects.requireNonNull(emitter);
        this.executor = Objects.requireNonNull(executor);
        this.s3Driver = Objects.requireNonNull(s3Driver);
        this.renderer = Objects.requireNonNull(renderer);
    }

    @Override
    public Single<Void> onNext(InputMessage message) {
        return Single.just(message)
                .map(inputMapper::transform)
                .flatMap(this::onTileRenderRequested);
    }

    private Single<Void> onTileRenderRequested(TileRenderRequested event) {
        return renderImage(event).map(result -> createEvent(event, makeStatus(result)))
                .map(outputMapper::transform)
                .flatMap(message -> emitter.send(message, Render.getTopicName(emitter.getTopicName() + "-completed", event.getLevel())));
    }

    private String makeStatus(Result result) {
        return (result.getError().isPresent() || result.getImage().length == 0) ? "FAILED" : "COMPLETED";
    }

    private TileRenderCompleted createEvent(TileRenderRequested event, String status) {
        return TileRenderCompleted.builder()
                .withDesignId(event.getDesignId())
                .withCommandId(event.getCommandId())
                .withRevision(event.getRevision())
                .withChecksum(event.getChecksum())
                .withLevel(event.getLevel())
                .withRow(event.getRow())
                .withCol(event.getCol())
                .withStatus(status)
                .build();
    }

    private Single<Result> uploadImage(TileRenderRequested event, Result result) {
        return s3Driver.putObject(createBucketKey(event), result.getImage())
                .doOnSuccess(response -> log.info("Image uploaded: " + createBucketKey(event)))
                .doOnError(response -> log.info("Can't upload image: " + createBucketKey(event)))
                .map(response -> result);
    }

    private Single<Result> renderImage(TileRenderRequested event) {
        return s3Driver.getObject(createBucketKey(event))
                .doOnError(err -> log.debug("Image not found: " + createBucketKey(event)))
                .map(image -> Result.of(image, null))
                .doOnSuccess(result -> log.info("Image found: " + createBucketKey(event)))
                .onErrorResumeNext(err -> renderImageBlocking(event).flatMap(result -> uploadImage(event, result)));
    }

    private Single<Result> renderImageBlocking(TileRenderRequested event) {
        return executor.rxExecuteBlocking(promise -> {
            log.debug("Render image: " + createBucketKey(event));
            renderer.renderImage(event, promise);
            log.debug("Image rendered: " + createBucketKey(event));
        }, false);
    }
}
