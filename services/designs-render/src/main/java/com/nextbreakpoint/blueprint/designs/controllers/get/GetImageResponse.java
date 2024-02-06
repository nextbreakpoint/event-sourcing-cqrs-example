package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(setterPrefix = "with")
public class GetImageResponse {
    private final Image image;

    public GetImageResponse(Image image) {
        this.image = image;
    }

    public Optional<Image> getImage() {
        return Optional.ofNullable(image);
    }
}
