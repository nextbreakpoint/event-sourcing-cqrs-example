package com.nextbreakpoint.shop.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Design {
    private final String manifest;
    private final String metadata;
    private final String script;

    @JsonCreator
    public Design(@JsonProperty("manifest") String manifest,
                  @JsonProperty("metadata") String metadata,
                  @JsonProperty("script") String script) {
        this.manifest = manifest;
        this.metadata = metadata;
        this.script = script;
    }

    public String getManifest() {
        return manifest;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getScript() {
        return script;
    }
}
