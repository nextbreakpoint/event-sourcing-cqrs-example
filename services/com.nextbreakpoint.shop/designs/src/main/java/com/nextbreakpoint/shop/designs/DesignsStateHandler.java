package com.nextbreakpoint.shop.designs;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.util.Collections;
import java.util.List;

public class DesignsStateHandler extends StateHandler {
    public DesignsStateHandler(Store store) {
        super(store);
    }

    @Override
    protected Single<List<JsonObject>> getState(RoutingContext routingContext) {
        return getStore().getDesignsState().map(s -> s.map(this::makeState).map(Collections::singletonList).orElseGet(Collections::emptyList));
    }

    private JsonObject makeState(JsonObject x) {
        return makeState(x.getString("NAME"), x.getString("MODIFIED"));
    }
}
