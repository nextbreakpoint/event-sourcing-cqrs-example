package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteAccountRequestMapper implements Mapper<RoutingContext, DeleteAccountRequest> {
    @Override
    public DeleteAccountRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("param0");

        return new DeleteAccountRequest(UUID.fromString(uuid));
    }
}
