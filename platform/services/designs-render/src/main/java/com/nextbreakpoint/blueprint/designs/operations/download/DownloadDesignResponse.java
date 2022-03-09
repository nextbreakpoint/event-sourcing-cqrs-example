package com.nextbreakpoint.blueprint.designs.operations.download;

import java.util.Objects;

public class DownloadDesignResponse {
    private final byte[] bytes;

    public DownloadDesignResponse(byte[] bytes) {
        this.bytes = Objects.requireNonNull(bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }
}
