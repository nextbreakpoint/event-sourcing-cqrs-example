package com.nextbreakpoint.blueprint.accounts.controllers.delete;

import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class DeleteAccountRequestMapper implements Mapper<RoutingContext, DeleteAccountRequest> {
    @Override
    public DeleteAccountRequest transform(RoutingContext context) {
        final String uuid = context.request().getParam("accountId");

        return new DeleteAccountRequest(UUID.fromString(uuid));
    }
}
