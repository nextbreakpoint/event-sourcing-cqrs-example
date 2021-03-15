package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.DelegateConsumer;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import com.nextbreakpoint.blueprint.designs.controllers.list.ListDesignsController;
import com.nextbreakpoint.blueprint.designs.controllers.list.ListDesignsInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.list.ListDesignsOutputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.load.LoadDesignController;
import com.nextbreakpoint.blueprint.designs.controllers.load.LoadDesignInputMapper;
import com.nextbreakpoint.blueprint.designs.controllers.load.LoadDesignOutputMapper;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignResponse;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createListDesignsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListDesignsRequest, ListDesignsResponse, String>builder()
                .withInputMapper(new ListDesignsInputMapper())
                .withOutputMapper(new ListDesignsOutputMapper())
                .withController(new ListDesignsController(store))
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadDesignHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadDesignRequest, LoadDesignResponse, Optional<String>>builder()
                .withInputMapper(new LoadDesignInputMapper())
                .withOutputMapper(new LoadDesignOutputMapper())
                .withController(new LoadDesignController(store))
                .onSuccess(new DelegateConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
