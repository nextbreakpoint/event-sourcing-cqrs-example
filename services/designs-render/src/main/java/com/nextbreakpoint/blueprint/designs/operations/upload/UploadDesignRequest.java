package com.nextbreakpoint.blueprint.designs.operations.upload;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class UploadDesignRequest {
    private String file;

    public UploadDesignRequest(String file) {
        this.file = Objects.requireNonNull(file);
    }
}
