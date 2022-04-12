package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;

import java.util.Optional;

public class LoadDesignResponseMapper implements Mapper<LoadDesignResponse, Optional<String>> {
    @Override
    public Optional<String> transform(LoadDesignResponse response) {
        return response.getDesign().map(DesignDocument::from).map(Json::encodeValue);
    }
}
