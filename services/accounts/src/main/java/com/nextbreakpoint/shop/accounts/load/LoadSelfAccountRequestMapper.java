package com.nextbreakpoint.shop.accounts.load;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class LoadSelfAccountRequestMapper implements RequestMapper<LoadAccountRequest> {
    @Override
    public LoadAccountRequest apply(RoutingContext context) {
        final String uuid = context.user().principal().getString("user");

        return new LoadAccountRequest(UUID.fromString(uuid));
    }
}
