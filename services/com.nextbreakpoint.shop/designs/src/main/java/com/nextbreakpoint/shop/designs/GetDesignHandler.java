package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.Headers.MODIFIED;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;

public class GetDesignHandler implements Handler<RoutingContext> {
    private Store store;

    public GetDesignHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processGetDesign(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processGetDesign(RoutingContext routingContext) {
        final UUID uuid = UUID.fromString(routingContext.request().getParam("param0"));

        store.loadDesign(uuid).subscribe(result -> emitGetDesignResponse(routingContext, result.orElse(null)), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitGetDesignResponse(RoutingContext routingContext, JsonObject result) {
        if (result == null) {
            routingContext.response().setStatusCode(404).end();
        } else {
            try {
                final String uuid = result.getString("UUID");
                final String json = result.getString("JSON");
                final String created = result.getString("CREATED");
                final String updated = result.getString("UPDATED");

                final JsonObject parsedJson = new JsonObject(json);
                final String manifest = parsedJson.getString("manifest");
                final String metadata = parsedJson.getString("metadata");
                final String script = parsedJson.getString("script");

                final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);

                final Date modifiedValue = df.parse(updated);

                routingContext.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .putHeader(MODIFIED, String.valueOf(modifiedValue.getTime()))
                        .setStatusCode(200)
                        .end(createDesignResponseObject(UUID.fromString(uuid), created, updated, manifest, metadata, script).encode());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private JsonObject createDesignResponseObject(UUID uuid, String created, String updated, String manifest, String metadata, String script) {
        final JsonObject json = new JsonObject();
        json.put("uuid", uuid.toString());
        json.put("created", created);
        json.put("updated", updated);
        json.put("manifest", manifest);
        json.put("metadata", metadata);
        json.put("script", script);
        return json;
    }
}