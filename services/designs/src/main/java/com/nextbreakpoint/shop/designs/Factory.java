package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.vertx.handlers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.designs.common.ContentConsumer;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignController;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.delete.DeleteDesignResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.get.GetStatusController;
import com.nextbreakpoint.shop.designs.controllers.get.GetStatusRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.get.GetStatusResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignController;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.insert.InsertDesignResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsController;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListStatusController;
import com.nextbreakpoint.shop.designs.controllers.list.ListStatusRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListStatusResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignController;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignResponseMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignController;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignRequestMapper;
import com.nextbreakpoint.shop.designs.controllers.update.UpdateDesignResponseMapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.GetStatusRequest;
import com.nextbreakpoint.shop.designs.model.GetStatusResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.ListStatusRequest;
import com.nextbreakpoint.shop.designs.model.ListStatusResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

public class Factory {
    private Factory() {}

    public static DefaultHandler<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, Content> createDeleteDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteDesignRequest, DeleteDesignResponse, Content>builder()
                .withInputMapper(new DeleteDesignRequestMapper())
                .withOutputMapper(new DeleteDesignResponseMapper())
                .withController(new DeleteDesignController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, InsertDesignRequest, InsertDesignResponse, Content> createInsertDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, InsertDesignRequest, InsertDesignResponse, Content>builder()
                .withInputMapper(new InsertDesignRequestMapper())
                .withOutputMapper(new InsertDesignResponseMapper())
                .withController(new InsertDesignController(store))
                .onSuccess(new ContentConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, Content> createUpdateDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, UpdateDesignRequest, UpdateDesignResponse, Content>builder()
                .withInputMapper(new UpdateDesignRequestMapper())
                .withOutputMapper(new UpdateDesignResponseMapper())
                .withController(new UpdateDesignController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content> createListDesignsHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withOutputMapper(new ListDesignsResponseMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content> createLoadDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withOutputMapper(new LoadDesignResponseMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, GetStatusRequest, GetStatusResponse, Content> createGetStatusHandler(Store store) {
        return DefaultHandler.<RoutingContext, GetStatusRequest, GetStatusResponse, Content>builder()
                .withInputMapper(new GetStatusRequestMapper())
                .withOutputMapper(new GetStatusResponseMapper())
                .withController(new GetStatusController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, ListStatusRequest, ListStatusResponse, Content> createListStatusHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListStatusRequest, ListStatusResponse, Content>builder()
                .withInputMapper(new ListStatusRequestMapper())
                .withOutputMapper(new ListStatusResponseMapper())
                .withController(new ListStatusController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
