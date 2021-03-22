package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.NotFoundConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsController;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignController;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequestMapper;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponseMapper;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponse;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsRequestMapper())
                .withController(new ListDesignsController(store))
                .withOutputMapper(new ListDesignsResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignRequestMapper())
                .withController(new LoadDesignController(store))
                .withOutputMapper(new LoadDesignResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
