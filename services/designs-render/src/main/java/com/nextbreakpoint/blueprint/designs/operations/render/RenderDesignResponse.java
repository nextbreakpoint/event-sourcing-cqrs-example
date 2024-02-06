package com.nextbreakpoint.blueprint.designs.operations.render;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class RenderDesignResponse {
    private final List<String> errors;
    private final String checksum;

    public RenderDesignResponse(List<String> errors, String checksum) {
        this.errors = Objects.requireNonNull(errors);
        this.checksum = checksum;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
