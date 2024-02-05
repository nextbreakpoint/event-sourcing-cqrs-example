package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;

public class GetImageRequestMapper implements Mapper<RoutingContext, GetImageRequest> {
    @Override
    public GetImageRequest transform(RoutingContext context) {
        final String checksum = context.pathParam("checksum");

        if (checksum == null) {
            throw new IllegalStateException("the request doesn't have the required path parameter: checksum is missing");
        }

        return GetImageRequest.builder()
                .withChecksum(checksum)
                .build();
    }
}
