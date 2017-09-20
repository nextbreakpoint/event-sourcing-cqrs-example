package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.Headers.MODIFIED;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_EXT_PATTERN;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;

public class ListDesignsHandler implements Handler<RoutingContext> {
    private Store store;

    public ListDesignsHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processListDesigns(routingContext);
        } catch (Exception e) {
            e.printStackTrace();

            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processListDesigns(RoutingContext routingContext) {
        store.findDesigns().subscribe(result -> emitListDesignsResponse(routingContext, result), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitListDesignsResponse(RoutingContext routingContext, List<JsonObject> result) {
        try {
            final JsonArray output = result.stream().map(x -> x.getString("UUID")).collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b));

            final String modified = result.stream().findFirst().map(json -> json.getString("MODIFIED")).orElse("");

            if (modified.equals("")) {
                routingContext.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .putHeader(MODIFIED, String.valueOf(new Date().getTime()))
                        .setStatusCode(200)
                        .end(output.encode());
            } else {
                final SimpleDateFormat df = new SimpleDateFormat(modified.length() > 20 ? TIMESTAMP_EXT_PATTERN : TIMESTAMP_PATTERN);

                final Date modifiedValue = df.parse(modified);

                routingContext.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .putHeader(MODIFIED, String.valueOf(modifiedValue.getTime()))
                        .setStatusCode(200)
                        .end(output.encode());
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
