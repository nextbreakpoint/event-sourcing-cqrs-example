package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.common.vertx.DesignChangedMapper;
import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.designs.controllers.change.DesignChangeInputMapper;
import com.nextbreakpoint.shop.designs.controllers.change.DesignChangedController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignInputMapper;
import com.nextbreakpoint.shop.designs.model.CommandOutput;
import com.nextbreakpoint.shop.designs.model.RecordAndMessage;
import io.vertx.core.Handler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

import java.util.function.BiConsumer;

public class Factory {
    private Factory() {}

    public static Handler<RecordAndMessage> createInsertDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, CommandOutput> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, InsertDesignCommand, DesignChangedEvent, CommandOutput>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(event -> new CommandOutput(event))
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }

    public static Handler<RecordAndMessage> createUpdateDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, CommandOutput> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, UpdateDesignCommand, DesignChangedEvent, CommandOutput>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(event -> new CommandOutput(event))
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }

    public static Handler<RecordAndMessage> createDeleteDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, CommandOutput> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, DeleteDesignCommand, DesignChangedEvent, CommandOutput>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(event -> new CommandOutput(event))
                .withController(new DeleteDesignController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }

    public static Handler<RecordAndMessage> createDesignChangedHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource, BiConsumer<RecordAndMessage, DesignChangedEvent> onSuccess, BiConsumer<RecordAndMessage, Throwable> onFailure) {
        return TemplateHandler.<RecordAndMessage, DesignChangedEvent, DesignChangedEvent, DesignChangedEvent>builder()
                .withInputMapper(new DesignChangeInputMapper())
                .withOutputMapper(event -> event)
                .withController(new DesignChangedController(store, topic, producer, new DesignChangedMapper(messageSource)))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
                .build();
    }
}
