package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.EventHandler;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndEvent;
import com.nextbreakpoint.blueprint.common.vertx.RecordAndMessage;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.*;
import com.nextbreakpoint.blueprint.designs.events.*;
import com.nextbreakpoint.blueprint.designs.common.AggregateUpdateCompletedMessageMapper;
import com.nextbreakpoint.blueprint.designs.operations.AggregateUpdateRequestedController;
import com.nextbreakpoint.blueprint.designs.common.AggregateUpdateRequestedInputMapper;
import com.nextbreakpoint.blueprint.designs.common.AggregateUpdateRequestedMessageMapper;
import com.nextbreakpoint.blueprint.designs.operations.DesignUpdateRequestedController;
import com.nextbreakpoint.blueprint.designs.operations.TileRenderCompletedController;
import com.nextbreakpoint.blueprint.designs.common.TileRenderCompletedInputMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static EventHandler<RecordAndMessage> createDesignInsertRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, RecordAndEvent<DesignInsertRequested>, Void, JsonObject>builder()
                .withInputMapper(new DesignInsertRequestedInputMapper())
                .withOutputMapper(ignore -> new JsonObject())
                .withController(new DesignUpdateRequestedController<>(
                    new DesignInsertRequestedEventHandler(store),
                    new AggregateUpdateRequestedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new EventSuccessConsumer())
                .onFailure(new EventFailureConsumer())
                .build();
    }

    public static EventHandler<RecordAndMessage> createDesignUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, RecordAndEvent<DesignUpdateRequested>, Void, JsonObject>builder()
                .withInputMapper(new DesignUpdateRequestedInputMapper())
                .withOutputMapper(ignore -> new JsonObject())
                .withController(new DesignUpdateRequestedController<>(
                    new DesignUpdateRequestedEventHandler(store),
                    new AggregateUpdateRequestedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new EventSuccessConsumer())
                .onFailure(new EventFailureConsumer())
                .build();
    }

    public static EventHandler<RecordAndMessage> createDesignDeleteRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, RecordAndEvent<DesignDeleteRequested>, Void, JsonObject>builder()
                .withInputMapper(new DesignDeleteRequestedInputMapper())
                .withOutputMapper(ignore -> new JsonObject())
                .withController(new DesignUpdateRequestedController<>(
                    new DesignDeleteRequestedEventHandler(store),
                    new AggregateUpdateRequestedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new EventSuccessConsumer())
                .onFailure(new EventFailureConsumer())
                .build();
    }

    public static EventHandler<RecordAndMessage> createAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, RecordAndEvent<AggregateUpdateRequested>, Void, JsonObject>builder()
                .withInputMapper(new AggregateUpdateRequestedInputMapper())
                .withOutputMapper(ignore -> new JsonObject())
                .withController(new AggregateUpdateRequestedController(
                    store,
                    new AggregateUpdateCompletedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new EventSuccessConsumer())
                .onFailure(new EventFailureConsumer())
                .build();
    }

    public static EventHandler<RecordAndMessage> createTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, RecordAndEvent<TileRenderCompleted>, Void, JsonObject>builder()
                .withInputMapper(new TileRenderCompletedInputMapper())
                .withOutputMapper(event -> new JsonObject())
                .withController(new TileRenderCompletedController(
                        store,
                        new TileRenderUpdatedMessageMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new EventSuccessConsumer())
                .onFailure(new EventFailureConsumer())
                .build();
    }
}
