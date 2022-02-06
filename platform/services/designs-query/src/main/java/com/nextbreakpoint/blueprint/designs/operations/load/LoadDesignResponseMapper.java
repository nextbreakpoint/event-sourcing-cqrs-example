package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.persistence.LoadDesignResponse;
import io.vertx.core.json.Json;

import java.util.Optional;

public class LoadDesignResponseMapper implements Mapper<LoadDesignResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadDesignResponse response) {
        return response.getDesign().map(Json::encode);
    }
}
