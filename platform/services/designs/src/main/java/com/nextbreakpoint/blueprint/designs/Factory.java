package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.controllers.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.controllers.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.controllers.list.ListDesignsController;
import com.nextbreakpoint.blueprint.designs.controllers.list.ListDesignsRequestMapper;
import com.nextbreakpoint.blueprint.designs.controllers.list.ListDesignsResponseMapper;
import com.nextbreakpoint.blueprint.designs.controllers.load.LoadDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.load.LoadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.controllers.load.LoadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.controllers.update.UpdateDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.model.*;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createDeleteDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, String>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withOutputMapper(new DeleteDesignResponseMapper())
                .withController(new DeleteDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createInsertDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withOutputMapper(new InsertDesignResponseMapper())
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new JsonConsumer(201))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUpdateDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withOutputMapper(new UpdateDesignResponseMapper())
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withOutputMapper(new ListDesignsResponseMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static TemplateHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withOutputMapper(new LoadDesignResponseMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new DelegateConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
