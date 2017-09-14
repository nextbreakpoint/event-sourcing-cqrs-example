package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;

public abstract class StateHandler implements Handler<RoutingContext> {
    private Store store;

    protected StateHandler(Store store) {
        this.store = store;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            processState(routingContext);
        } catch (Exception e) {
            e.printStackTrace();

            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void processState(RoutingContext routingContext) {
        getState(routingContext).subscribe(results -> emitStateResponse(routingContext, results), err -> routingContext.fail(Failure.databaseError(err)));
    }

    private void emitStateResponse(RoutingContext routingContext, List<JsonObject> results) {
        if (results == null) {
            routingContext.response().setStatusCode(404);
        } else {
            try {
                final JsonArray state = new JsonArray(results);

                routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end(state.encode());
            } catch (Exception e) {
                routingContext.response().setStatusCode(500);
            }
        }
    }

    protected JsonObject makeState(String watchKey, String lastModified) {
        final JsonObject state = new JsonObject();

        final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);

        state.put("watch_key", watchKey);

        try {
            state.put("last_modified", df.parse(lastModified).getTime());
        } catch (ParseException e) {
            state.put("last_modified", 0);

            e.printStackTrace();
        }

        return state;
    }

    protected abstract Single<List<JsonObject>> getState(RoutingContext routingContext);

    protected Store getStore() {
        return store;
    }
}
