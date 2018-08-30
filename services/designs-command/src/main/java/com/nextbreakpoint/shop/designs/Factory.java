package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.vertx.handlers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.vertx.handlers.SimpleJsonConsumer;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignOutputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignOutputMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignMessageMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignOutputMapper;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static DefaultHandler<RoutingContext, InsertDesignCommand, CommandResult, String> createInsertDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, InsertDesignCommand, CommandResult, String>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(new InsertDesignOutputMapper())
                .withController(new InsertDesignController(topic, producer, new InsertDesignMessageMapper(messageSource)))
                .onSuccess(new SimpleJsonConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, UpdateDesignCommand, CommandResult, String> createUpdateDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, UpdateDesignCommand, CommandResult, String>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(new UpdateDesignOutputMapper())
                .withController(new UpdateDesignController(topic, producer, new UpdateDesignMessageMapper(messageSource)))
                .onSuccess(new SimpleJsonConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, DeleteDesignCommand, CommandResult, String> createDeleteDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return DefaultHandler.<RoutingContext, DeleteDesignCommand, CommandResult, String>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(new DeleteDesignOutputMapper())
                .withController(new DeleteDesignController(topic, producer, new DeleteDesignMessageMapper(messageSource)))
                .onSuccess(new SimpleJsonConsumer(202))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
