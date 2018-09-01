package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.common.vertx.consumers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.designs.common.ContentConsumer;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsController;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsInputMapper;
import com.nextbreakpoint.shop.designs.controllers.list.ListDesignsOutputMapper;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignController;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignInputMapper;
import com.nextbreakpoint.shop.designs.controllers.load.LoadDesignOutputMapper;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content>builder()
                .withInputMapper(new ListDesignsInputMapper())
                .withOutputMapper(new ListDesignsOutputMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content>builder()
                .withInputMapper(new LoadDesignInputMapper())
                .withOutputMapper(new LoadDesignOutputMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
