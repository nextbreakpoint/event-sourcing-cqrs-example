package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.*;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignCommand;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignController;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignCommand;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignController;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignCommand;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignController;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
import com.nextbreakpoint.blueprint.designs.model.ControllerResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<RecordAndMessage> createInsertDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, InsertDesignCommand, ControllerResult, JsonObject>builder()
                .withInputMapper(new InsertDesignInputMapper())
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }

    public static Handler<RecordAndMessage> createUpdateDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, UpdateDesignCommand, ControllerResult, JsonObject>builder()
                .withInputMapper(new UpdateDesignInputMapper())
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }

    public static Handler<RecordAndMessage> createDeleteDesignHandler(Store store, String topic, KafkaProducer<String, String> producer, String messageSource) {
        return TemplateHandler.<RecordAndMessage, DeleteDesignCommand, ControllerResult, JsonObject>builder()
                .withInputMapper(new DeleteDesignInputMapper())
                .withController(new DeleteDesignController(store, topic, producer, new DesignChangedMessageMapper(messageSource)))
                .withOutputMapper(event -> new JsonObject())
                .onSuccess(new MessaggeSuccessConsumer())
                .onFailure(new MessaggeFailureConsumer())
                .build();
    }
}
