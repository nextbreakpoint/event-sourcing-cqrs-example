package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.commands.mappers.DesignDeleteCommandInputMapper;
import com.nextbreakpoint.blueprint.common.commands.mappers.DesignDeleteCommandOutputMapper;
import com.nextbreakpoint.blueprint.common.commands.mappers.DesignInsertCommandInputMapper;
import com.nextbreakpoint.blueprint.common.commands.mappers.DesignInsertCommandOutputMapper;
import com.nextbreakpoint.blueprint.common.commands.mappers.DesignUpdateCommandInputMapper;
import com.nextbreakpoint.blueprint.common.commands.mappers.DesignUpdateCommandOutputMapper;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageEmitter;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.MessageConsumed;
import com.nextbreakpoint.blueprint.common.vertx.MessageFailed;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.controllers.DesignDeleteCommandController;
import com.nextbreakpoint.blueprint.designs.controllers.DesignInsertCommandController;
import com.nextbreakpoint.blueprint.designs.controllers.DesignUpdateCommandController;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignCommandMapper;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignController;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignCommandMapper;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignController;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignCommandMapper;
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

    public static Handler<RoutingContext> createInsertDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withController(new InsertDesignController(
                        new InsertDesignCommandMapper(),
                        new DesignInsertCommandOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .withOutputMapper(new InsertDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUpdateDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withController(new UpdateDesignController(
                        new UpdateDesignCommandMapper(),
                        new DesignUpdateCommandOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .withOutputMapper(new UpdateDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createDeleteDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, String>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withController(new DeleteDesignController(
                        new DeleteDesignCommandMapper(),
                        new DesignDeleteCommandOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .withOutputMapper(new DeleteDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignInsertCommandHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignInsertCommandController(
                        store,
                        new DesignInsertCommandInputMapper(),
                        new DesignInsertRequestedOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignUpdateCommandHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignUpdateCommandController(
                        store,
                        new DesignUpdateCommandInputMapper(),
                        new DesignUpdateRequestedOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }

    public static RxSingleHandler<InputMessage, ?> createDesignDeleteCommandHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<InputMessage, InputMessage, Void, Void>builder()
                .withInputMapper(input -> input)
                .withOutputMapper(output -> output)
                .withController(new DesignDeleteCommandController(
                        store,
                        new DesignDeleteCommandInputMapper(),
                        new DesignDeleteRequestedOutputMapper(messageSource),
                        new KafkaMessageEmitter(producer, BackendRegistries.getDefaultNow(), topic, 3)
                ))
                .onSuccess(new MessageConsumed())
                .onFailure(new MessageFailed())
                .build();
    }
}
