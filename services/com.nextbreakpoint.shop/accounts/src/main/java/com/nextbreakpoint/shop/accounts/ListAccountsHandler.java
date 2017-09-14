package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class ListAccountsHandler implements Handler<RoutingContext> {
    private Store store;

    public ListAccountsHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processListAccounts(routingContext);
        } catch (Exception e) {
            e.printStackTrace();

            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processListAccounts(RoutingContext routingContext) {
        final String lookupEmail = routingContext.request().getParam("email");

        if (lookupEmail == null) {
            store.findAccounts().subscribe(result -> emitListAccountsResponse(routingContext, result), err -> routingContext.fail(Failure.databaseError(err)));
        } else {
            store.findAccounts(lookupEmail).subscribe(result -> emitListAccountsResponse(routingContext, result), err -> routingContext.fail(Failure.databaseError(err)));
        }
    }

    private void emitListAccountsResponse(RoutingContext routingContext, List<JsonObject> result) {
        final JsonArray output = result.stream().map(x -> x.getString("UUID")).collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b));

        routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end(output.encode());
    }
}
