package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(setterPrefix = "with")
public class GetTileResponse {
    private final Image image;

    public GetTileResponse(Image image) {
        this.image = image;
    }

    public Optional<Image> getImage() {
        return Optional.ofNullable(image);
    }
}
