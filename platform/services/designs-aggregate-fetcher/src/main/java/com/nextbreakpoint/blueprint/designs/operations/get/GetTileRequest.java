package com.nextbreakpoint.blueprint.designs.operations.get;

import java.util.Objects;
import java.util.UUID;

public class GetTileRequest {
    private final UUID uuid;

    public GetTileRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
