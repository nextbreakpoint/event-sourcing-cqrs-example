package com.nextbreakpoint.blueprint.designs.controllers.load;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignResponse;
import io.vertx.core.json.Json;

import java.util.Optional;

public class LoadDesignOutputMapper implements Mapper<LoadDesignResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadDesignResponse response) {
        return response.getDesignDocument().map(Json::encode);
    }
}
