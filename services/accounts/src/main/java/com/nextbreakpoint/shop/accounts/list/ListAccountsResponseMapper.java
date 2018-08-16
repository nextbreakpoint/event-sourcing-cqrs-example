package com.nextbreakpoint.shop.accounts.list;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;
import io.vertx.core.json.JsonArray;

public class ListAccountsResponseMapper implements Mapper<ListAccountsResponse, Content> {
    @Override
    public Content transform(ListAccountsResponse response) {
        final String json = response.getUuids()
                .stream()
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        return new Content(json);
    }
}
