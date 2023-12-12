package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TileStatus;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.common.AsyncTileRenderer;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.common.Result;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

import static com.nextbreakpoint.blueprint.designs.common.Bucket.createBucketKey;
import static com.nextbreakpoint.blueprint.designs.common.Render.createRenderKey;

@Log4j2
public class TileRenderRequestedController implements Controller<InputMessage<TileRenderRequested>, Void> {
    private final MessageEmitter<TileRenderCompleted> emitter;
    private final S3Driver driver;
    private final AsyncTileRenderer renderer;
    private final String messageSource;

    public TileRenderRequestedController(String messageSource, MessageEmitter<TileRenderCompleted> emitter, S3Driver driver, AsyncTileRenderer renderer) {
        this.messageSource = Objects.requireNonNull(messageSource);
        this.emitter = Objects.requireNonNull(emitter);
        this.driver = Objects.requireNonNull(driver);
        this.renderer = Objects.requireNonNull(renderer);
    }

    @Override
    public Single<Void> onNext(InputMessage<TileRenderRequested> message) {
        return Single.fromCallable(() -> message.getValue().getData())
                .flatMap(this::onTileRenderRequested);
    }

    private Single<Void> onTileRenderRequested(TileRenderRequested event) {
        return renderImage(event)
                .map(result -> createEvent(event, makeStatus(result)))
                .map(this::createMessage)
                .flatMap(message -> emitter.send(message, Render.getTopicName(emitter.getTopicName() + "-completed", event.getLevel())));
    }

    private String makeStatus(Result result) {
        return (result.getError().isPresent() || result.getImage().length == 0) ? "FAILED" : "COMPLETED";
    }

    private OutputMessage<TileRenderCompleted> createMessage(TileRenderCompleted event) {
        return MessageFactory.<TileRenderCompleted>of(messageSource)
                .createOutputMessage(createRenderKey(event), event);
    }

    private TileRenderCompleted createEvent(TileRenderRequested event, String status) {
        return TileRenderCompleted.newBuilder()
                .setDesignId(event.getDesignId())
                .setCommandId(event.getCommandId())
                .setRevision(event.getRevision())
                .setChecksum(event.getChecksum())
                .setLevel(event.getLevel())
                .setRow(event.getRow())
                .setCol(event.getCol())
                .setStatus(TileStatus.valueOf(status))
                .build();
    }

    private Single<Result> renderImage(TileRenderRequested event) {
        return driver.getObject(createBucketKey(event))
                .doOnError(err -> log.debug("Image not found: {}", createBucketKey(event)))
                .map(image -> Result.of(image, null))
                .doOnSuccess(result -> log.info("Image found: {}", createBucketKey(event)))
                .onErrorResumeNext(err -> renderer.renderImage(event).flatMap(result -> uploadImage(event, result)));
    }

    private Single<Result> uploadImage(TileRenderRequested event, Result result) {
        return driver.putObject(createBucketKey(event), result.getImage())
                .doOnSuccess(response -> log.info("Image uploaded: {}", createBucketKey(event)))
                .doOnError(response -> log.info("Can't upload image: {}", createBucketKey(event)))
                .map(response -> result);
    }
}
