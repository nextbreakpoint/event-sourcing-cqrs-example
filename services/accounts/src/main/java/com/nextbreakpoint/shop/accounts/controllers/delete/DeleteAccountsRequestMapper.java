package com.nextbreakpoint.shop.accounts.controllers.delete;

import com.nextbreakpoint.shop.accounts.model.DeleteAccountsRequest;
import com.nextbreakpoint.shop.common.model.Mapper;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteAccountsRequestMapper implements Mapper<RoutingContext, DeleteAccountsRequest> {
    @Override
    public DeleteAccountsRequest transform(RoutingContext context) {
        return new DeleteAccountsRequest();
    }
}
