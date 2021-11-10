package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.NotFoundConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.common.DesignChangedMessageMapper;
import com.nextbreakpoint.blueprint.designs.operations.delete.*;
import com.nextbreakpoint.blueprint.designs.operations.insert.*;
import com.nextbreakpoint.blueprint.designs.operations.list.*;
import com.nextbreakpoint.blueprint.designs.operations.load.*;
import com.nextbreakpoint.blueprint.designs.operations.update.*;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createInsertDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMessageMapper(source)))
                .withOutputMapper(new InsertDesignResponseMapper())
                .onSuccess(new JsonConsumer(201))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUpdateDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMessageMapper(source)))
                .withOutputMapper(new UpdateDesignResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createDeleteDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, String>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withController(new DeleteDesignController(store, topic, producer, new DesignChangedMessageMapper(source)))
                .withOutputMapper(new DeleteDesignResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withController(new ListDesignsController(store))
                .withOutputMapper(new ListDesignsResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static TemplateHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withController(new LoadDesignController(store))
                .withOutputMapper(new LoadDesignResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
