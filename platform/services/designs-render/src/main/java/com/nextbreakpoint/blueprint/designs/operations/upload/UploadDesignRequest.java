package com.nextbreakpoint.blueprint.designs.operations.upload;

import java.util.Objects;

public class UploadDesignRequest {
    private String file;

    public UploadDesignRequest(String file) {
        this.file = Objects.requireNonNull(file);
    }

    public String getFile() {
        return file;
    }
}
