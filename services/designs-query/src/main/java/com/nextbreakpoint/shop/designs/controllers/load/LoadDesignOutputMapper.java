package com.nextbreakpoint.shop.designs.controllers.load;

import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import io.vertx.core.json.Json;

import java.util.Optional;

public class LoadDesignOutputMapper implements Mapper<LoadDesignResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadDesignResponse response) {
        return response.getDesignDocument().map(Json::encode);
    }
}
