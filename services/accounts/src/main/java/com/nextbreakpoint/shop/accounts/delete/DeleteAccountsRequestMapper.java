package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.RequestMapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteAccountsRequestMapper implements RequestMapper<DeleteAccountsRequest> {
    @Override
    public DeleteAccountsRequest apply(RoutingContext context) {
        return new DeleteAccountsRequest();
    }
}
