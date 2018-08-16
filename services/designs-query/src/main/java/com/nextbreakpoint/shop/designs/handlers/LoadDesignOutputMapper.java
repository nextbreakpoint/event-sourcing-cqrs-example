package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Metadata;
import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.Set;

import static com.nextbreakpoint.shop.common.Metadata.MODIFIED;
import static java.util.Collections.singleton;

public class LoadDesignOutputMapper implements Mapper<LoadDesignResponse, Content> {
    @Override
    public Content transform(LoadDesignResponse response) {
        final Set<Metadata> metadata = response.getDesign()
                .map(design -> singleton(new Metadata(MODIFIED, String.valueOf(design.getModified()))))
                .orElse(Collections.emptySet());

        final String json = response.getDesign()
                .map(design -> new JsonObject()
                        .put("uuid", design.getUuid())
                        .put("json", design.getJson())
                        .put("created", design.getCreated())
                        .put("updated", design.getUpdated())
                        .encode())
                .orElse(null);

        return new Content(json, metadata);
    }
}
