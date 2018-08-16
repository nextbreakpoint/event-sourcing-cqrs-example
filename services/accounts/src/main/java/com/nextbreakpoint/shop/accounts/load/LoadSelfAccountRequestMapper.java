package com.nextbreakpoint.shop.accounts.load;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadSelfAccountRequestMapper implements Mapper<RoutingContext, LoadAccountRequest> {
    @Override
    public LoadAccountRequest transform(RoutingContext context) {
        final String uuid = context.user().principal().getString("user");

        return new LoadAccountRequest(UUID.fromString(uuid));
    }
}
