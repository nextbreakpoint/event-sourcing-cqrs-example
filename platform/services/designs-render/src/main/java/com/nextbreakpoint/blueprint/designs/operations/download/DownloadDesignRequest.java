package com.nextbreakpoint.blueprint.designs.operations.download;

import java.util.Objects;

public class DownloadDesignRequest {
    private final String manifest;
    private final String metadata;
    private final String script;

    public DownloadDesignRequest(String manifest, String metadata, String script) {
        this.manifest = Objects.requireNonNull(manifest);
        this.metadata = Objects.requireNonNull(metadata);
        this.script = Objects.requireNonNull(script);
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
