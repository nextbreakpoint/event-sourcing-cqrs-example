package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.events.mappers.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.controllers.*;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static MessageHandler<Message, Void> createDesignInsertRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    store,
                    new DesignAggregateUpdateRequestedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createDesignUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    store,
                    new DesignAggregateUpdateRequestedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createDesignDeleteRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    store,
                    new DesignAggregateUpdateRequestedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createDesignAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateRequestedController(
                    store,
                    new DesignAggregateUpdateRequestedInputMapper(),
                    new DesignAggregateUpdateCompletedMessageMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createDesignAggregateUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateCompletedController(
                        store,
                        new DesignAggregateUpdateCompletedInputMapper(),
                        new TileRenderRequestedMessageMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createTileAggregateUpdateRequiredHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateRequiredController(
                        store,
                        new TileAggregateUpdateRequiredInputMapper(),
                        new TileAggregateUpdateRequestedMessageMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createTileAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateRequestedController(
                        store,
                        new TileAggregateUpdateRequestedInputMapper(),
                        new TileAggregateUpdateCompletedMessageMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createTileAggregateUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateCompletedController())
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createTileRenderRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderRequestedController())
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<Message, Void> createTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<Message, Message, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderCompletedController(
                        store,
                        new TileRenderCompletedInputMapper(),
                        new TileAggregateUpdateRequiredMessageMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }
}
