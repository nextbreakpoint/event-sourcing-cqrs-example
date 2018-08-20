package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.handlers.ContentConsumer;
import com.nextbreakpoint.shop.common.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.handlers.FailedRequestConsumer;
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
import io.vertx.rxjava.ext.web.RoutingContext;

public class Factory {
    private Factory() {}

    public static DefaultHandler<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content> createListDesignsHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, Content>builder()
                .withInputMapper(new ListDesignsInputMapper())
                .withOutputMapper(new ListDesignsOutputMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content> createLoadDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content>builder()
                .withInputMapper(new LoadDesignInputMapper())
                .withOutputMapper(new LoadDesignOutputMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
