package com.nextbreakpoint.blueprint.designs.operations.get;

import java.util.Optional;

public class GetTileResponse {
    private final byte[] data;

    public GetTileResponse(byte[] data) {
        this.data = data;
    }

    public Optional<byte[]> getData() {
        return Optional.ofNullable(data);
    }
}
