package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Metadata;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import io.vertx.core.json.Json;

import java.util.Set;

import static com.nextbreakpoint.shop.common.Metadata.MODIFIED;
import static java.util.Collections.singleton;

public class ListDesignsOutputMapper implements Mapper<ListDesignsResponse, Content> {
    @Override
    public Content transform(ListDesignsResponse response) {
        final String json = Json.encode(response.getUuids());

        final String modified = String.valueOf(response.getUpdated());

        final Set<Metadata> metadata = singleton(new Metadata(MODIFIED, modified));

        return new Content(json, metadata);
    }
}
