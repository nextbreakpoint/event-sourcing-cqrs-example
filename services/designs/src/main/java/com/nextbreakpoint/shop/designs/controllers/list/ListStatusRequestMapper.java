package com.nextbreakpoint.shop.designs.controllers.list;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.ListStatusRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

public class ListStatusRequestMapper implements Mapper<RoutingContext, ListStatusRequest> {
    @Override
    public ListStatusRequest transform(RoutingContext context) {
        final JsonArray jsonArray = context.getBodyAsJsonArray();

        final List<String> uuids = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            uuids.add(jsonArray.getString(i));
        }

        return new ListStatusRequest(uuids);
    }
}
