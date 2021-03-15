package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignMessageMapper;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignOutputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignMessageMapper;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignOutputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignMessageMapper;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignOutputMapper;
import com.nextbreakpoint.blueprint.designs.model.CommandResult;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createInsertDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, InsertDesign, CommandResult, String>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withOutputMapper(new InsertDesignOutputMapper())
                .withController(new InsertDesignController(topic, producer, new InsertDesignMessageMapper(messageSource)))
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUpdateDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, UpdateDesign, CommandResult, String>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withOutputMapper(new UpdateDesignOutputMapper())
                .withController(new UpdateDesignController(topic, producer, new UpdateDesignMessageMapper(messageSource)))
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createDeleteDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, DeleteDesign, CommandResult, String>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withOutputMapper(new DeleteDesignOutputMapper())
                .withController(new DeleteDesignController(topic, producer, new DeleteDesignMessageMapper(messageSource)))
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
