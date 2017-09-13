package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class DeleteAccountHandler implements Handler<RoutingContext> {
    private Store store;

    public DeleteAccountHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processRemoveAccount(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processRemoveAccount(RoutingContext routingContext) {
        final UUID uuid = UUID.fromString(routingContext.request().getParam("param0"));

        store.deleteAccount(uuid).subscribe(event -> emitDeleteAccountResponse(routingContext, uuid), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitDeleteAccountResponse(RoutingContext routingContext, UUID uuid) {
        routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end(deleteAccountResponseObject(uuid).encode());
    }

    private JsonObject deleteAccountResponseObject(UUID uuid) {
        final JsonObject json = new JsonObject();
        json.put("uuid", uuid.toString());
        return json;
    }
}
