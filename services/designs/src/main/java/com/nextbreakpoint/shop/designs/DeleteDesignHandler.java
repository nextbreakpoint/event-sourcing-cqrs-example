package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class DeleteDesignHandler implements Handler<RoutingContext> {
    private Store store;

    public DeleteDesignHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processRemoveDesign(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processRemoveDesign(RoutingContext routingContext) {
        final UUID uuid = UUID.fromString(routingContext.request().getParam("param0"));

        store.deleteDesign(uuid).subscribe(event -> emitDeleteDesignResponse(routingContext, uuid), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitDeleteDesignResponse(RoutingContext routingContext, UUID uuid) {
        routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end(createDesignResponseObject(uuid).encode());
    }

    private JsonObject createDesignResponseObject(UUID uuid) {
        final JsonObject json = new JsonObject();
        json.put("uuid", uuid.toString());
        return json;
    }
}
