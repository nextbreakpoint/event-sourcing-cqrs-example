package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class UpdateDesignHandler implements Handler<RoutingContext> {
    private Store store;

    public UpdateDesignHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processUpdateDesign(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processUpdateDesign(RoutingContext routingContext) {
        final JsonObject body = routingContext.getBodyAsJson();

        final UUID uuid = UUID.fromString(routingContext.pathParam("param0"));

        final JsonObject design = new JsonObject();

        design.put("manifest", body.getString("manifest"));
        design.put("metadata", body.getString("metadata"));
        design.put("script", body.getString("script"));

        BundleUtil.parseBundle(design).flatMap(bundle -> store.updateDesign(uuid, design))
                .subscribe(event -> emitUpdateDesignResponse(routingContext, uuid), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitUpdateDesignResponse(RoutingContext routingContext, UUID uuid) {
        routingContext.response().setStatusCode(200).end();
    }
}
