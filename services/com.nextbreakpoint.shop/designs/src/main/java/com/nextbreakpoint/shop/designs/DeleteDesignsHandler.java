package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class DeleteDesignsHandler implements Handler<RoutingContext> {
    private Store store;

    public DeleteDesignsHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processRemoveDesigns(routingContext);
        } catch (Exception e) {
            e.printStackTrace();

            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processRemoveDesigns(RoutingContext routingContext) {
        store.deleteDesigns().subscribe(event -> emitDeleteDesignsResponse(routingContext), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitDeleteDesignsResponse(RoutingContext routingContext) {
        routingContext.response().setStatusCode(204).end();
    }
}
