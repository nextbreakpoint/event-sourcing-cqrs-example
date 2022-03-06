package com.nextbreakpoint.blueprint.designs.persistence.dto;

import com.nextbreakpoint.blueprint.designs.model.Design;

import java.util.Objects;
import java.util.UUID;

public class InsertDesignRequest {
    private final UUID uuid;
    private final Design design;
    private boolean draft;

    public InsertDesignRequest(UUID uuid, Design design, boolean draft) {
        this.uuid = Objects.requireNonNull(uuid);
        this.design = Objects.requireNonNull(design);
        this.draft = draft;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Design getDesign() {
        return design;
    }

    public boolean isDraft() {
        return draft;
    }
}
