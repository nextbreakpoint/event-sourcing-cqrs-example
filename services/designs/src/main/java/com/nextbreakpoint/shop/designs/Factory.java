package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.vertx.handlers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.vertx.handlers.OptionalConsumer;
import com.nextbreakpoint.shop.common.vertx.handlers.SimpleJsonConsumer;
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
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static DefaultHandler<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, String> createDeleteDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, String>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withOutputMapper(new DeleteDesignResponseMapper())
                .withController(new DeleteDesignController(store))
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, InsertDesignRequest, InsertDesignResponse, String> createInsertDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, String>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withOutputMapper(new InsertDesignResponseMapper())
                .withController(new InsertDesignController(store))
                .onSuccess(new SimpleJsonConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String> createUpdateDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, String>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withOutputMapper(new UpdateDesignResponseMapper())
                .withController(new UpdateDesignController(store))
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, ListDesignsRequest, ListDesignsResponse, String> createListDesignsHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withOutputMapper(new ListDesignsResponseMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>> createLoadDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withOutputMapper(new LoadDesignResponseMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new OptionalConsumer<>(new SimpleJsonConsumer(200)))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
