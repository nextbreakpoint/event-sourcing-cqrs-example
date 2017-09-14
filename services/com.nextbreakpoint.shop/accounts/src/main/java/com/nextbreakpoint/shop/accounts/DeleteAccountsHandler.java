package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteAccountsHandler implements Handler<RoutingContext> {
    private Store store;

    public DeleteAccountsHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processRemoveAccounts(routingContext);
        } catch (Exception e) {
            e.printStackTrace();

            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processRemoveAccounts(RoutingContext routingContext) {
        store.deleteAccounts().subscribe(event -> emitDeleteAccountsResponse(routingContext), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitDeleteAccountsResponse(RoutingContext routingContext) {
        routingContext.response().setStatusCode(204).end();
    }
}
