package com.nextbreakpoint.blueprint.designs.operations.validate;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class ValidateDesignRequest {
    private final String manifest;
    private final String metadata;
    private final String script;

    public ValidateDesignRequest(String manifest, String metadata, String script) {
        this.manifest = Objects.requireNonNull(manifest);
        this.metadata = Objects.requireNonNull(metadata);
        this.script = Objects.requireNonNull(script);
    }
}
