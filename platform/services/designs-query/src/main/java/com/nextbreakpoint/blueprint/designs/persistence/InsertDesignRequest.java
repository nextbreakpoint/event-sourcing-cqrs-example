package com.nextbreakpoint.blueprint.designs.persistence;

import com.nextbreakpoint.blueprint.designs.model.Design;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignRequest {
    private final UUID uuid;
    private final Design design;

    public InsertDesignRequest(UUID uuid, Design design) {
        this.uuid = Objects.requireNonNull(uuid);
        this.design = Objects.requireNonNull(design);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Design getDesign() {
        return design;
    }
}
