package com.nextbreakpoint.blueprint.accounts.operations.list;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.core.json.JsonArray;

public class ListAccountsResponseMapper implements Mapper<ListAccountsResponse, String> {
    @Override
    public String transform(ListAccountsResponse response) {
        return response.getUuids()
                .stream()
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll)
                .encode();
    }
}
