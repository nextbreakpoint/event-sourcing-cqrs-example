package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedMessageMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.controllers.TileRenderRequestedController;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class Factory {
    private Factory() {}

    public static EventHandler<Message, Void> createTileRenderRequestedHandler(String topic, KafkaProducer<String, String> producer, String messageSource, WorkerExecutor executor, S3AsyncClient s3AsyncClient, String bucket) {
        return TemplateHandler.<Message, TileRenderRequested, Void, Void>builder()
                .withInputMapper(new TileRenderRequestedInputMapper())
                .withOutputMapper(ignore -> null)
                .withController(new TileRenderRequestedController(
                        new TileRenderCompletedMessageMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3),
                        executor,
                        new S3Driver(s3AsyncClient, bucket),
                        new TileRenderer()
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }
}
