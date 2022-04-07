package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.ForwardController;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.drivers.KafkaTombstoneEmitter;
import com.nextbreakpoint.blueprint.common.events.mappers.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignStateStrategy;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.controllers.*;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.List;

public class Factory {
    private Factory() {}

    public static RxSingleHandler<InputMessage, ?> createDesignInsertRequestedHandler(Store store, String eventsTopic, String cancelTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignInsertRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignInsertRequestedInputMapper(),
                    new DesignAggregateUpdatedOutputMapper(messageSource),
                    new TileRenderCancelledOutputMapper(messageSource, Render::createRenderKey),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new KafkaMessageEmitter(producer, eventsTopic, 3),
                    new KafkaMessageEmitter(producer, cancelTopic, 3),
                    new KafkaMessageEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignUpdateRequestedHandler(Store store, String eventsTopic, String cancelTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignUpdateRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignUpdateRequestedInputMapper(),
                    new DesignAggregateUpdatedOutputMapper(messageSource),
                    new TileRenderCancelledOutputMapper(messageSource, Render::createRenderKey),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new KafkaMessageEmitter(producer, eventsTopic, 3),
                    new KafkaMessageEmitter(producer, cancelTopic, 3),
                    new KafkaMessageEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignDeleteRequestedHandler(Store store, String eventsTopic, String cancelTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignDeleteRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignDeleteRequestedInputMapper(),
                    new DesignAggregateUpdatedOutputMapper(messageSource),
                    new TileRenderCancelledOutputMapper(messageSource, Render::createRenderKey),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new KafkaMessageEmitter(producer, eventsTopic, 3),
                    new KafkaMessageEmitter(producer, cancelTopic, 3),
                    new KafkaMessageEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateTilesUpdateCompletedHandler(String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdatedController(
                    new DesignAggregateUpdatedInputMapper(),
                    new DesignDocumentUpdateRequestedOutputMapper(messageSource),
                    new DesignDocumentDeleteRequestedOutputMapper(messageSource),
                    new KafkaMessageEmitter(producer, topic, 3)
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
                    new KafkaMessageEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessagesConsumed())
                .onFailure(new MessagesFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createTilesRenderedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TilesRenderedController(
                        new DesignAggregate(store, new DesignStateStrategy()),
                        new TilesRenderedInputMapper(),
                        new DesignAggregateUpdatedOutputMapper(messageSource),
                        new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                        new KafkaMessageEmitter(producer, eventsTopic, 3),
                        new KafkaMessageEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createTileRenderCancelledHandler(String cancelTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderCancelledController(
                        new TileRenderCancelledInputMapper(),
                        new TileRenderCancelledOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, cancelTopic, 3),
                        new KafkaTombstoneEmitter(producer, renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createForwardTileRenderCompletedHandler(String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new ForwardController<>(
                        new TileRenderCompletedInputMapper(),
                        new TileRenderCompletedOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
