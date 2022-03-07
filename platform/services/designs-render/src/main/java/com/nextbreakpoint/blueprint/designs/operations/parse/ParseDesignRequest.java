package com.nextbreakpoint.blueprint.designs.operations.parse;

import java.util.Objects;

public class ParseDesignRequest {
    private String file;

    public ParseDesignRequest(String file) {
        this.file = Objects.requireNonNull(file);
    }

    public String getFile() {
        return file;
    }
}
