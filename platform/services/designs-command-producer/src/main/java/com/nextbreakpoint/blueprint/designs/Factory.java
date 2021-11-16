package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.KafkaEmitter;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.operations.delete.*;
import com.nextbreakpoint.blueprint.designs.operations.insert.*;
import com.nextbreakpoint.blueprint.designs.operations.update.*;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createInsertDesignHandler(KafkaProducer<String, String> producer, String topic, String messageSource) {
        return TemplateHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withController(new InsertDesignController(
                        request -> new DesignInsertRequested(Uuids.timeBased(), request.getUuid(), request.getJson(), request.getLevels()),
                        new DesignInsertRequestedOutputMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
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
                        request -> new DesignUpdateRequested(Uuids.timeBased(), request.getUuid(), request.getJson(), request.getLevels()),
                        new DesignUpdateRequestedOutputMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
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
                        request -> new DesignDeleteRequested(Uuids.timeBased(), request.getUuid()),
                        new DesignDeleteRequestedOutputMapper(messageSource),
                        new KafkaEmitter(producer, topic, 3)
                ))
                .withOutputMapper(new DeleteDesignResponseMapper())
                .onSuccess(new JsonConsumer(202))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
