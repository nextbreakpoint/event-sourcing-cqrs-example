package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteAccountsRequestMapper implements Mapper<RoutingContext, DeleteAccountsRequest> {
    @Override
    public DeleteAccountsRequest transform(RoutingContext context) {
        return new DeleteAccountsRequest();
    }
}
