package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdatedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdatedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TilesRenderedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TilesRenderedOutputMapper;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.MessagesConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessagesFailed;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignStateStrategy;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.controllers.BufferedTileRenderCompletedController;
import com.nextbreakpoint.blueprint.designs.controllers.DesignAggregateUpdatedController;
import com.nextbreakpoint.blueprint.designs.controllers.DesignUpdateController;
import com.nextbreakpoint.blueprint.designs.controllers.TileRenderCompletedController;
import com.nextbreakpoint.blueprint.designs.controllers.TilesRenderedController;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.List;

public class Factory {
    private Factory() {}

    public static RxSingleHandler<InputMessage, ?> createDesignInsertRequestedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignInsertRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignInsertRequestedInputMapper(),
                    new DesignAggregateUpdatedOutputMapper(messageSource),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), eventsTopic, 3),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignUpdateRequestedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignUpdateRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignUpdateRequestedInputMapper(),
                    new DesignAggregateUpdatedOutputMapper(messageSource),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), eventsTopic, 3),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignDeleteRequestedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignDeleteRequestedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new DesignDeleteRequestedInputMapper(),
                    new DesignAggregateUpdatedOutputMapper(messageSource),
                    new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), eventsTopic, 3),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignAggregateUpdatedHandler(String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdatedController(
                    new DesignAggregateUpdatedInputMapper(),
                    new DesignDocumentUpdateRequestedOutputMapper(messageSource),
                    new DesignDocumentDeleteRequestedOutputMapper(messageSource),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<List<InputMessage>, ?> createBufferedTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<List<InputMessage>, List<InputMessage>, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new BufferedTileRenderCompletedController(
                    new DesignAggregate(store, new DesignStateStrategy()),
                    new TileRenderCompletedInputMapper(),
                    new TilesRenderedOutputMapper(messageSource),
                    new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .onSuccess(new MessagesConsumed())
                .onFailure(new MessagesFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createTileRenderCompletedHandler(Store store, String bufferTopic, String renderTopic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new TileRenderCompletedController(
                        new DesignAggregate(store, new DesignStateStrategy()),
                        new TileRenderCompletedInputMapper(),
                        new TileRenderCompletedOutputMapper(messageSource),
                        new TileRenderRequestedOutputMapper(messageSource, Render::createRenderKey),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), bufferTopic, 3),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), renderTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
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
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), eventsTopic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
