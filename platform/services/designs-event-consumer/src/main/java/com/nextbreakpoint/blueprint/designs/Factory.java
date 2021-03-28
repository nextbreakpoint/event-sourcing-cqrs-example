package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.*;
import com.nextbreakpoint.blueprint.designs.model.*;
import com.nextbreakpoint.blueprint.designs.operations.design.DesignChangeInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.design.DesignChangedController;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderCreatedController;
import com.nextbreakpoint.blueprint.designs.operations.render.RenderCreatedInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.tile.TileCompletedController;
import com.nextbreakpoint.blueprint.designs.operations.tile.TileCompletedInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.version.VersionCreatedController;
import com.nextbreakpoint.blueprint.designs.operations.version.VersionCreatedInputMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<RecordAndMessage> createDesignChangedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, DesignChanged, ControllerResult, JsonObject>builder()
                .withInputMapper(new DesignChangeInputMapper())
                .withController(new DesignChangedController(store, topic, producer, new VersionCreatedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }

    public static Handler<RecordAndMessage> createVersionCreatedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, VersionCreated, ControllerResult, JsonObject>builder()
                .withInputMapper(new VersionCreatedInputMapper())
                .withController(new VersionCreatedController(store, topic, producer, new RenderCreatedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }

    public static Handler<RecordAndMessage> createRenderCreatedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, RenderCreated, ControllerResult, JsonObject>builder()
                .withInputMapper(new RenderCreatedInputMapper())
                .withController(new RenderCreatedController(store, topic, producer, new TileCreatedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }

    public static Handler<RecordAndMessage> createTileCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, TileCompleted, ControllerResult, JsonObject>builder()
                .withInputMapper(new TileCompletedInputMapper())
                .withController(new TileCompletedController(store, topic, producer, new RenderCompletedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }
}
