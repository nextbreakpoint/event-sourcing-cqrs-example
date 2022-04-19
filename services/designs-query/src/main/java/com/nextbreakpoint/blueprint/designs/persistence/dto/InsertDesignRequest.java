package com.nextbreakpoint.blueprint.designs.persistence.dto;

import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class InsertDesignRequest {
    private final UUID uuid;
    private final Design design;
    private final boolean draft;

    public InsertDesignRequest(UUID uuid, Design design, boolean draft) {
        this.uuid = Objects.requireNonNull(uuid);
        this.design = Objects.requireNonNull(design);
        this.draft = draft;
    }
}
