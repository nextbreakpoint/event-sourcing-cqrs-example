package com.nextbreakpoint.shop.designs.list;

import com.nextbreakpoint.shop.common.Header;
import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonArray;

import java.util.Set;

import static com.nextbreakpoint.shop.common.Headers.MODIFIED;
import static java.util.Collections.singleton;

public class ListDesignsResponseMapper implements ResponseMapper<ListDesignsResponse> {
    @Override
    public Result apply(ListDesignsResponse response) {
        final String json = response.getUuids()
                .stream()
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        final String modified = String.valueOf(response.getUpdated());

        final Set<Header> headers = singleton(new Header(MODIFIED, modified));

        return new Result(json, headers);
    }
}
