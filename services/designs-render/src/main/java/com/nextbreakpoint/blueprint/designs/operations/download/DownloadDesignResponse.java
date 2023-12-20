package com.nextbreakpoint.blueprint.designs.operations.download;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(setterPrefix = "with")
public class DownloadDesignResponse {
    private final byte[] bytes;

    public DownloadDesignResponse(byte[] bytes) {
        this.bytes = bytes;
    }

    public Optional<byte[]> getBytes() {
        return Optional.ofNullable(bytes);
    }
}
