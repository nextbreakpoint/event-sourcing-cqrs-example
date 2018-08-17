package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.DefaultHandler;
import com.nextbreakpoint.shop.common.RequestFailedHandler;
import com.nextbreakpoint.shop.designs.handlers.ListDesignsController;
import com.nextbreakpoint.shop.designs.handlers.ListDesignsInputMapper;
import com.nextbreakpoint.shop.designs.handlers.ListDesignsOutputMapper;
import com.nextbreakpoint.shop.designs.handlers.LoadDesignController;
import com.nextbreakpoint.shop.designs.handlers.LoadDesignInputMapper;
import com.nextbreakpoint.shop.designs.handlers.LoadDesignOutputMapper;
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
                .onSuccess(new ContentHandler(200))
                .onFailure(new RequestFailedHandler())
                .build();
    }

    public static DefaultHandler<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content> createLoadDesignHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Content>builder()
                .withInputMapper(new LoadDesignInputMapper())
                .withOutputMapper(new LoadDesignOutputMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new ContentHandler(200, 404))
                .onFailure(new RequestFailedHandler())
                .build();
    }
}
