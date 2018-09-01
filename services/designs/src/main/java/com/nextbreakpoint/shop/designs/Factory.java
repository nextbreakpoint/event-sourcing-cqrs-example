package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.DesignChangedMapper;
import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.common.vertx.consumers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.vertx.consumers.OptionalConsumer;
import com.nextbreakpoint.shop.common.vertx.consumers.SimpleJsonConsumer;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsController;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignController;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignResponseMapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
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
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createInsertDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withOutputMapper(new InsertDesignResponseMapper())
                .withController(new InsertDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new SimpleJsonConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createUpdateDesignHandler(Store store, String topic, String source, KafkaProducer<String, String> producer) {
        return TemplateHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withOutputMapper(new UpdateDesignResponseMapper())
                .withController(new UpdateDesignController(store, topic, producer, new DesignChangedMapper(source)))
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withOutputMapper(new ListDesignsResponseMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static TemplateHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withOutputMapper(new LoadDesignResponseMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new OptionalConsumer<>(new SimpleJsonConsumer(200)))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
