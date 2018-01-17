package com.nextbreakpoint.shop.accounts.list;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListAccountsRequestMapper implements RequestMapper<ListAccountsRequest> {
    @Override
    public ListAccountsRequest apply(RoutingContext context) {
        final String email = context.request().getParam("email");

        return new ListAccountsRequest(email);
    }
}
