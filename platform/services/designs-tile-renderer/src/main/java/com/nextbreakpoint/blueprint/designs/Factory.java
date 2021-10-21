package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.TileCompletedMessageMapper;
import com.nextbreakpoint.blueprint.designs.common.MessaggeFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.MessaggeSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.model.ControllerResult;
import com.nextbreakpoint.blueprint.designs.model.TileCreated;
import com.nextbreakpoint.blueprint.designs.operations.CreateTileInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.CreateTileController;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class Factory {
    private Factory() {}

    public static Handler<RecordAndMessage> createTileCreatedHandler(WorkerExecutor executor, S3AsyncClient s3AsyncClient, String bucket, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, TileCreated, ControllerResult, JsonObject>builder()
                .withInputMapper(new CreateTileInputMapper())
                .withController(new CreateTileController(executor, s3AsyncClient, bucket, topic, producer, new TileCompletedMessageMapper(messageSource)))
                .withOutputMapper(result -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }
}
