package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Messages;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.MessagesConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessagesFailed;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignMergeStrategy;
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

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignInsertRequestedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignInsertRequested>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignInsertRequested) data))
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignInsertRequestedController(
                        messageSource,
                        new DesignEventStore(store, new DesignMergeStrategy()),
                        createEventMessageEmitter(producer, eventsTopic),
                        createEventMessageEmitter(producer, renderTopic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignUpdateRequestedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignUpdateRequested>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignUpdateRequested) data))
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignUpdateRequestedController(
                        messageSource,
                        new DesignEventStore(store, new DesignMergeStrategy()),
                        createEventMessageEmitter(producer, eventsTopic),
                        createEventMessageEmitter(producer, renderTopic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDeleteRequestedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignDeleteRequested>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignDeleteRequested) data))
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateController.DesignDeleteRequestedController(
                        messageSource,
                        new DesignEventStore(store, new DesignMergeStrategy()),
                        createEventMessageEmitter(producer, eventsTopic),
                        createEventMessageEmitter(producer, renderTopic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignAggregateUpdatedHandler(String topic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignAggregateUpdated>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignAggregateUpdated) data))
                .withOutputMapper(output -> output)
                .withController(new DesignAggregateUpdatedController(
                        messageSource,
                        createEventMessageEmitter(producer, topic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<List<InputMessage<Object>>, Void> createBufferedTileRenderCompletedHandler(Store store, String topic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<List<InputMessage<Object>>, List<InputMessage<TileRenderCompleted>>, Void, Void>builder()
                .withInputMapper(messages -> messages.stream().map(message -> Messages.asSpecificMessage(message, data -> (TileRenderCompleted) data)).toList())
                .withOutputMapper(output -> output)
                .withController(new BufferedTileRenderCompletedController(
                        messageSource,
                        new DesignEventStore(store, new DesignMergeStrategy()),
                        createEventMessageEmitter(producer, topic)
                ))
                .onSuccess(new MessagesConsumed<>())
                .onFailure(new MessagesFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createTileRenderCompletedHandler(Store store, String bufferTopic, String renderTopic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<TileRenderCompleted>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (TileRenderCompleted) data))
                .withOutputMapper(output -> output)
                .withController(new TileRenderCompletedController(
                        messageSource,
                        new DesignEventStore(store, new DesignMergeStrategy()),
                        createEventMessageEmitter(producer, bufferTopic),
                        createEventMessageEmitter(producer, renderTopic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createTilesRenderedHandler(Store store, String eventsTopic, String renderTopic, KafkaProducer<String, Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<TilesRendered>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (TilesRendered) data))
                .withOutputMapper(output -> output)
                .withController(new TilesRenderedController(
                        messageSource,
                        new DesignEventStore(store, new DesignMergeStrategy()),
                        createEventMessageEmitter(producer, eventsTopic)
                ))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    private static <T> KafkaMessageEmitter<Payload, T> createEventMessageEmitter(KafkaProducer<String, Payload> producer, String topic) {
        return new KafkaMessageEmitter<>(producer, Records.createEventOutputRecordMapper(), BackendRegistries.getDefaultNow(), topic, 3);
    }
}
