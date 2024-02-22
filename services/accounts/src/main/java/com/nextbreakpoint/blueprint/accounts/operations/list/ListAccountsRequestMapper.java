package com.nextbreakpoint.blueprint.accounts.operations.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListAccountsRequestMapper implements Mapper<RoutingContext, ListAccountsRequest> {
    @Override
    public ListAccountsRequest transform(RoutingContext context) {
        final String login = context.request().getParam("login");

        return new ListAccountsRequest(login);
    }
}
