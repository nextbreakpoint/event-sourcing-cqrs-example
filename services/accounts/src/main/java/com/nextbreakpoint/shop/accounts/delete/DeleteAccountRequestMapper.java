package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteAccountRequestMapper implements RequestMapper<DeleteAccountRequest> {
    @Override
    public DeleteAccountRequest apply(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new DeleteAccountRequest(UUID.fromString(uuid));
    }
}
