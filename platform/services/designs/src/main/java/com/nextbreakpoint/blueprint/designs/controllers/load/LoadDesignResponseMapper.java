package com.nextbreakpoint.blueprint.designs.controllers.load;

import com.nextbreakpoint.blueprint.common.core.DesignDocument;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignResponse;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class LoadDesignResponseMapper implements Mapper<LoadDesignResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadDesignResponse response) {
        final Optional<String> json = response.getDesign()
                .map(design -> createObject(design).encode());

        return json;
    }

    private JsonObject createObject(DesignDocument design) {
        return new JsonObject()
                .put("uuid", design.getUuid())
                .put("json", design.getJson())
                .put("modified", design.getModified())
                .put("checksum", design.getChecksum());
    }
}
