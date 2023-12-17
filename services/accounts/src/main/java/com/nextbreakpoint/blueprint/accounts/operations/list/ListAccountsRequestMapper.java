package com.nextbreakpoint.blueprint.accounts.operations.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListAccountsRequestMapper implements Mapper<RoutingContext, ListAccountsRequest> {
    @Override
    public ListAccountsRequest transform(RoutingContext context) {
        final String email = context.request().getParam("email");

        return new ListAccountsRequest(email);
    }
}
