package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;

public class CreateDesignHandler implements Handler<RoutingContext> {
    private Store store;

    public CreateDesignHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processCreateDesign(routingContext);
        } catch (Exception e) {
            e.printStackTrace();

            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processCreateDesign(RoutingContext routingContext) {
        final JsonObject body = routingContext.getBodyAsJson();

        final UUID uuid = UUID.randomUUID();

        BundleUtil.parseBundle(body).flatMap(bundle -> store.insertDesign(uuid, body))
                .subscribe(event -> emitInsertDesignResponse(routingContext, uuid), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitInsertDesignResponse(RoutingContext routingContext, UUID uuid) {
        routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(201).end(createDesignResponseObject(uuid).encode());
    }

    private JsonObject createDesignResponseObject(UUID uuid) {
        final JsonObject json = new JsonObject();
        json.put("uuid", uuid.toString());
        return json;
    }
}
