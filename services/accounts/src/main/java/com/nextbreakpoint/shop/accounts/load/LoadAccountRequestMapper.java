package com.nextbreakpoint.shop.accounts.load;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadAccountRequestMapper implements RequestMapper<LoadAccountRequest> {
    @Override
    public LoadAccountRequest apply(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new LoadAccountRequest(UUID.fromString(uuid));
    }
}
