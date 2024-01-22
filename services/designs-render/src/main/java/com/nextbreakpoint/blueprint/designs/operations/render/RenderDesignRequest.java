package com.nextbreakpoint.blueprint.designs.operations.render;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class RenderDesignRequest {
    private final String manifest;
    private final String metadata;
    private final String script;

    public RenderDesignRequest(String manifest, String metadata, String script) {
        this.manifest = Objects.requireNonNull(manifest);
        this.metadata = Objects.requireNonNull(metadata);
        this.script = Objects.requireNonNull(script);
    }
}
