package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class CreateAccountHandler implements Handler<RoutingContext> {
    private Store store;

    public CreateAccountHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processCreateAccount(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processCreateAccount(RoutingContext routingContext) {
        final JsonObject body = routingContext.getBodyAsJson();

        final UUID uuid = UUID.randomUUID();

        store.insertAccount(uuid, body.getString("name"), body.getString("email"), body.getString("role"))
                .subscribe(event -> emitInsertAccountResponse(routingContext, uuid, body.getString("role")), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitInsertAccountResponse(RoutingContext routingContext, UUID uuid, String role) {
        routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(201).end(createAccountResponseObject(uuid, role).encode());
    }

    private JsonObject createAccountResponseObject(UUID uuid, String role) {
        final JsonObject json = new JsonObject();
        json.put("uuid", uuid.toString());
        json.put("role", role);
        return json;
    }
}
