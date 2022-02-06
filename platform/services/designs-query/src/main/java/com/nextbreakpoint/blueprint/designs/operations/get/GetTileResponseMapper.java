package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.common.core.Mapper;

import java.util.Optional;

public class GetTileResponseMapper implements Mapper<GetTileResponse, Optional<Image>> {
    @Override
    public Optional<Image> transform(GetTileResponse response) {
        return response.getImage();
    }
}
