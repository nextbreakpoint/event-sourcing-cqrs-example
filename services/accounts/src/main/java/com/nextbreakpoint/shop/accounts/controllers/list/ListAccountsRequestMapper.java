package com.nextbreakpoint.shop.accounts.controllers.list;

import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ListAccountsRequestMapper implements Mapper<RoutingContext, ListAccountsRequest> {
    @Override
    public ListAccountsRequest transform(RoutingContext context) {
        final String email = context.request().getParam("email");

        return new ListAccountsRequest(email);
    }
}
