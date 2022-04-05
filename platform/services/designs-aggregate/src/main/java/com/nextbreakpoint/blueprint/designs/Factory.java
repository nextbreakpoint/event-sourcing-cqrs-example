package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.mappers.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignStateStrategy;
import com.nextbreakpoint.blueprint.common.vertx.ForwardController;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.controllers.*;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

import java.util.List;

public class Factory {
    private Factory() {}

    public static RxSingleHandler<InputMessage, ?> createDesignInsertRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignInsertRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignInsertRequestedInputMapper(),
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignUpdateRequestedInputMapper(),
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignDeleteRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignDeleteRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignDeleteRequestedInputMapper(),
                    new DesignAggregateUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateUpdateCancelledHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateCancelledController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignAggregateUpdateCancelledInputMapper(),
                    new TileRenderAbortedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignAggregateUpdateRequestedInputMapper(),
                    new DesignAggregateUpdateCompletedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateUpdateCompletedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdateCompletedController(
                    new DesignAggregateUpdateCompletedInputMapper(),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new DesignDocumentDeleteRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, eventsTopic, 3),
                    new KafkaEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateTilesUpdateRequestedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateTilesUpdateRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignAggregateTilesUpdateRequestedInputMapper(),
                    new DesignAggregateTilesUpdateCompletedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateTilesUpdateCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateTilesUpdateCompletedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignAggregateTilesUpdateCompletedInputMapper(),
                    new DesignDocumentUpdateRequestedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<List<InputMessage>, ?> createTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<List<InputMessage>, List<InputMessage>, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderCompletedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new TileRenderCompletedInputMapper(),
                    new TilesRenderedOutputMapper(messageSource),
                    new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessagesConsumed())
                .onFailure(new MessagesFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createForwardTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new ForwardController<>(
                        new TileRenderCompletedInputMapper(),
                        new TileRenderCompletedOutputMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createTilesRenderedHandler(Store store, String eventTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TilesRenderedController(
                        new DesignAggregate(store, new DesignStateStrategy()),
                        new TilesRenderedInputMapper(),
                        new DesignAggregateTilesUpdateRequestedOutputMapper(messageSource),
                        new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                        new KafkaEmitter(producer, eventTopic, 3),
                        new KafkaEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
