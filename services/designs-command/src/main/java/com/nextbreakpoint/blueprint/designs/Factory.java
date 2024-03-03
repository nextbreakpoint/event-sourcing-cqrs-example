package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.Payload;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Messages;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.DesignsRenderClient;
import com.nextbreakpoint.blueprint.designs.controllers.DesignCommandController;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignController;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignController;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignController;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignResponseMapper;
import io.vertx.core.Handler;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.apache.kafka.clients.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createInsertDesignHandler(KafkaProducer<String, Payload> producer, String topic, String messageSource, DesignsRenderClient designsRenderClient) {
        return TemplateHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withController(new InsertDesignController(messageSource, createCommandMessageEmitter(producer, topic), designsRenderClient))
                .withOutputMapper(new InsertDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUpdateDesignHandler(KafkaProducer<String, Payload> producer, String topic, String messageSource, DesignsRenderClient designsRenderClient) {
        return TemplateHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withController(new UpdateDesignController(messageSource, createCommandMessageEmitter(producer, topic), designsRenderClient))
                .withOutputMapper(new UpdateDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createDeleteDesignHandler(KafkaProducer<String, com.nextbreakpoint.blueprint.common.commands.avro.Payload> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, String>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withController(new DeleteDesignController(messageSource, createCommandMessageEmitter(producer, topic)))
                .withOutputMapper(new DeleteDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignInsertCommandHandler(Store store, String topic, KafkaProducer<String, com.nextbreakpoint.blueprint.common.events.avro.Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignInsertCommand>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignInsertCommand) data))
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController.DesignInsertCommandController(messageSource, store, createEventMessageEmitter(producer, topic)))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignUpdateCommandHandler(Store store, String topic, KafkaProducer<String, com.nextbreakpoint.blueprint.common.events.avro.Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignUpdateCommand>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignUpdateCommand) data))
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController.DesignUpdateCommandController(messageSource, store, createEventMessageEmitter(producer, topic)))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    public static RxSingleHandler<InputMessage<Object>, Void> createDesignDeleteCommandHandler(Store store, String topic, KafkaProducer<String, com.nextbreakpoint.blueprint.common.events.avro.Payload> producer, String messageSource) {
        return TemplateHandler.<InputMessage<Object>, InputMessage<DesignDeleteCommand>, Void, Void>builder()
                .withInputMapper(message -> Messages.asSpecificMessage(message, data -> (DesignDeleteCommand) data))
                .withOutputMapper(output -> output)
                .withController(new DesignCommandController.DesignDeleteCommandController(messageSource, store, createEventMessageEmitter(producer, topic)))
                .onSuccess(new MessageConsumed<>())
                .onFailure(new MessageFailed<>())
                .build();
    }

    private static <T> KafkaMessageEmitter<Payload, T> createCommandMessageEmitter(KafkaProducer<String, com.nextbreakpoint.blueprint.common.commands.avro.Payload> producer, String topic) {
        return new KafkaMessageEmitter<>(producer, Records.createCommandOutputRecordMapper(), BackendRegistries.getDefaultNow(), topic, 3);
    }

    private static <T> KafkaMessageEmitter<com.nextbreakpoint.blueprint.common.events.avro.Payload, T> createEventMessageEmitter(KafkaProducer<String, com.nextbreakpoint.blueprint.common.events.avro.Payload> producer, String topic) {
        return new KafkaMessageEmitter<>(producer, Records.createEventOutputRecordMapper(), BackendRegistries.getDefaultNow(), topic, 3);
    }
}
