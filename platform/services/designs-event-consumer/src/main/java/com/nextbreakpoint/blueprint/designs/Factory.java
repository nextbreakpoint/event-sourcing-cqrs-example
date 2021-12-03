package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.BlockingHandler;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.mappers.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregateManager;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregateStrategy;
import com.nextbreakpoint.blueprint.designs.common.Bucket;
import com.nextbreakpoint.blueprint.designs.controllers.*;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static BlockingHandler<InputMessage> createDesignInsertRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignDeleteRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignAbortRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAbortRequestedController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new DesignAbortRequestedInputMessageMapper(),
                    new TileRenderAbortedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateRequestedController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new DesignAggregateUpdateRequestedInputMapper(),
                    new DesignAggregateUpdateCompletedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createDesignAggregateUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateCompletedController(
                        new DesignAggregateUpdateCompletedInputMapper(),
                    new TileRenderRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileAggregateUpdateRequiredHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateRequiredController(
                        new TileAggregateUpdateRequiredInputMapper(),
                    new TileAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateRequestedController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new TileAggregateUpdateRequestedInputMapper(),
                    new TileAggregateUpdateCompletedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileAggregateUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileAggregateUpdateCompletedController())
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileRenderRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderRequestedController(
                    new TileRenderRequestedInputMapper(),
                    new TileRenderRequestedOutputMapper(messageSource, Bucket::createBucketKey),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderCompletedController(
                    new DesignAggregateManager(store, new DesignAggregateStrategy()),
                    new TileRenderCompletedInputMapper(),
                    new TileAggregateUpdateRequiredOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static BlockingHandler<InputMessage> createTileRenderAbortedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderAbortedController(
                    new TileRenderAbortedInputMapper(),
                    new TombstoneEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
