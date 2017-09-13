package com.nextbreakpoint.shop.designs;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.util.List;
import java.util.stream.Collectors;

public class DesignStateHandler extends StateHandler {
    public DesignStateHandler(Store store) {
        super(store);
    }

    @Override
    protected Single<List<JsonObject>> getState(RoutingContext routingContext) {
        return getStore().getDesignState(routingContext.getBodyAsJsonArray()).map(s -> s.stream().map(this::makeState).collect(Collectors.toList()));
    }

    private JsonObject makeState(JsonObject x) {
        return makeState(x.getString("UUID"), x.getString("UPDATED"));
    }
}
