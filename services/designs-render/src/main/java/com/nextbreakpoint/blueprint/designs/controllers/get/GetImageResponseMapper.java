package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.common.core.Mapper;

import java.util.Optional;

public class GetImageResponseMapper implements Mapper<GetImageResponse, Optional<Image>> {
    @Override
    public Optional<Image> transform(GetImageResponse response) {
        return response.getImage();
    }
}
