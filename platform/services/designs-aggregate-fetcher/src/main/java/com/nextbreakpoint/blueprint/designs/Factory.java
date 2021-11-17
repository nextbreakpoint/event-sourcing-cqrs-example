package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.operations.get.*;
import com.nextbreakpoint.blueprint.designs.operations.list.*;
import com.nextbreakpoint.blueprint.designs.operations.load.*;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;
import software.amazon.awssdk.services.s3.S3AsyncClient;

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

    public static Handler<RoutingContext> createGetTileHandler(Store store, S3AsyncClient s3AsyncClient, String s3Bucket) {
        return TemplateHandler.<RoutingContext, GetTileRequest, GetTileResponse, Optional<byte[]>>builder()
                .withInputMapper(new GetTileRequestMapper())
                .withController(new GetTileController(store, new S3Driver(s3AsyncClient, s3Bucket)))
                .withOutputMapper(new GetTileResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new PNGConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
