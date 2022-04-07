package com.nextbreakpoint.blueprint.designs.operations.upload;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class UploadDesignResponse {
    private final String manifest;
    private final String metadata;
    private final String script;
    private final List<String> errors;

    public UploadDesignResponse(String manifest, String metadata, String script, List<String> errors) {
        this.manifest = manifest;
        this.metadata = metadata;
        this.script = script;
        this.errors = Objects.requireNonNull(errors);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
