package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;

public class GetImageRequestMapper implements Mapper<RoutingContext, GetImageRequest> {
    @Override
    public GetImageRequest transform(RoutingContext context) {
        final List<String> checksum = context.queryParam("checksum");

        if (checksum == null || checksum.isEmpty()) {
            throw new IllegalStateException("the request doesn't have the required query parameter: checksum is missing");
        }

        return GetImageRequest.builder()
                .withChecksum(checksum.getFirst())
                .build();
    }
}
