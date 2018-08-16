package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.ContentHandler;
import com.nextbreakpoint.shop.common.FailedRequestHandler;
import com.nextbreakpoint.shop.common.RESTContentHandler;
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

public class Factory {
    private Factory() {}

    public static RESTContentHandler<ListDesignsRequest, ListDesignsResponse> createListDesignsHandler(Store store) {
        return RESTContentHandler.<ListDesignsRequest, ListDesignsResponse>builder()
                .withInputMapper(new ListDesignsInputMapper())
                .withOutputMapper(new ListDesignsOutputMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new ContentHandler(200))
                .onFailure(new FailedRequestHandler())
                .build();
    }

    public static RESTContentHandler<LoadDesignRequest, LoadDesignResponse> createLoadDesignHandler(Store store) {
        return RESTContentHandler.<LoadDesignRequest, LoadDesignResponse>builder()
                .withInputMapper(new LoadDesignInputMapper())
                .withOutputMapper(new LoadDesignOutputMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new ContentHandler(200, 404))
                .onFailure(new FailedRequestHandler())
                .build();
    }
}
