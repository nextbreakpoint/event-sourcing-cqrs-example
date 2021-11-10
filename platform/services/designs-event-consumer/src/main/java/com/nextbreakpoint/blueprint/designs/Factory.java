package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.mappers.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.Bucket;
import com.nextbreakpoint.blueprint.designs.controllers.*;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static MessageHandler<InputMessage, Void> createDesignInsertRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    store,
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createDesignUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    store,
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createDesignDeleteRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    store,
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createDesignAbortRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAbortRequestedController(
                    store,
                    new DesignAbortRequestedInputMessageMapper(),
                    new TileRenderAbortedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createDesignAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateRequestedController(
                    store,
                    new DesignAggregateUpdateRequestedInputMapper(),
                    new DesignAggregateUpdateCompletedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createDesignAggregateUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateCompletedController(
                    store,
                    new DesignAggregateUpdateCompletedInputMapper(),
                    new TileRenderRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createTileAggregateUpdateRequiredHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateRequiredController(
                    store,
                    new TileAggregateUpdateRequiredInputMapper(),
                    new TileAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createTileAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateRequestedController(
                    store,
                    new TileAggregateUpdateRequestedInputMapper(),
                    new TileAggregateUpdateCompletedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createTileAggregateUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateCompletedController())
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createTileRenderRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderRequestedController(
                    new TileRenderRequestedInputMapper(),
                    new TileRenderRequestedOutputMapper(messageSource, Bucket::createBucketKey),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderCompletedController(
                    store,
                    new TileRenderCompletedInputMapper(),
                    new TileAggregateUpdateRequiredOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }

    public static MessageHandler<InputMessage, Void> createTileRenderAbortedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderAbortedController(
                    new TileRenderAbortedInputMapper(),
                    new TombstoneEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageSuccessConsumer())
                .onFailure(new MessageFailureConsumer())
                .build();
    }
}
