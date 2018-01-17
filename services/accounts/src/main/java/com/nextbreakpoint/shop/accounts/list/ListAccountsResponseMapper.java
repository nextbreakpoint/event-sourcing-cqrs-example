package com.nextbreakpoint.shop.accounts.list;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonArray;

public class ListAccountsResponseMapper implements ResponseMapper<ListAccountsResponse> {
    @Override
    public Result apply(ListAccountsResponse response) {
        final String json = response.getUuids()
                .stream()
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        return new Result(json);
    }
}
