package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.CommandOutput;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.vertx.DesignChangedMapper;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.controllers.change.DesignChangeInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.change.DesignChangedController;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignInputMapper;
import io.vertx.core.Handler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

import java.util.function.BiConsumer;

public class Factory {
    private Factory() {}

    public static Handler<RecordAndMessage> createInsertDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, CommandOutput> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, InsertDesign, DesignChanged, CommandOutput>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(event -> new CommandOutput(event))
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }

    public static Handler<RecordAndMessage> createUpdateDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, CommandOutput> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, UpdateDesign, DesignChanged, CommandOutput>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(event -> new CommandOutput(event))
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }

    public static Handler<RecordAndMessage> createDeleteDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, CommandOutput> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, DeleteDesign, DesignChanged, CommandOutput>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(event -> new CommandOutput(event))
                .withController(new DeleteDesignController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }

    public static Handler<RecordAndMessage> createDesignChangedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, DesignChanged> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, DesignChanged, DesignChanged, DesignChanged>builder()
                .withInputMapper(new DesignChangeInputMapper())
                .withOutputMapper(event -> event)
                .withController(new DesignChangedController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }
}
