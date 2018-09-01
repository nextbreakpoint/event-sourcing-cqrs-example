package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.common.vertx.consumers.FailedMessageConsumer;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.common.CommandResultConsumer;
import com.nextbreakpoint.shop.common.vertx.DesignChangedMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignInputMapper;
import com.nextbreakpoint.shop.designs.model.CommandResult;
import io.vertx.core.Handler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<Message> createInsertDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<Message, InsertDesignCommand, CommandResult, CommandResult>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(result -> result)
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new CommandResultConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }

    public static Handler<Message> createUpdateDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<Message, UpdateDesignCommand, CommandResult, CommandResult>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(result -> result)
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new CommandResultConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }

    public static Handler<Message> createDeleteDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<Message, DeleteDesignCommand, CommandResult, CommandResult>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(result -> result)
                .withController(new DeleteDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new CommandResultConsumer())
                .onFailure(new FailedMessageConsumer())
                .build();
    }
}
