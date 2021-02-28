package com.nextbreakpoint.blueprint.accounts.controllers.list;

import com.nextbreakpoint.blueprint.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonArray;

public class ListAccountsResponseMapper implements Mapper<ListAccountsResponse, String> {
    @Override
    public String transform(ListAccountsResponse response) {
        final String json = response.getUuids()
                .stream()
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        return json;
    }
}
