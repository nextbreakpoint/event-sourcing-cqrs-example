package com.nextbreakpoint.shop.accounts.list;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListAccountsRequestMapper implements Mapper<RoutingContext, ListAccountsRequest> {
    @Override
    public ListAccountsRequest transform(RoutingContext context) {
        final String email = context.request().getParam("email");

        return new ListAccountsRequest(email);
    }
}
