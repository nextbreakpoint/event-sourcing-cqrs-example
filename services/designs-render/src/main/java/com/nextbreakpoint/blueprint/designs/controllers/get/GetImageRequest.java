package com.nextbreakpoint.blueprint.designs.controllers.get;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class GetImageRequest {
    private final String checksum;

    public GetImageRequest(String checksum) {
        this.checksum = Objects.requireNonNull(checksum);
    }
}
