package com.nextbreakpoint.shop.designs.controllers.list;

import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Metadata;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import io.vertx.core.json.JsonArray;

import java.util.Set;

import static com.nextbreakpoint.shop.common.model.Metadata.MODIFIED;
import static java.util.Collections.singleton;

public class ListDesignsResponseMapper implements Mapper<ListDesignsResponse, Content> {
    @Override
    public Content transform(ListDesignsResponse response) {
        final String json = response.getUuids()
                .stream()
                .collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b))
                .encode();

        final String modified = String.valueOf(response.getUpdated());

        final Set<Metadata> metadata = singleton(new Metadata(MODIFIED, modified));

        return new Content(json, metadata);
    }
}
