package com.nextbreakpoint.shop.designs.model;

import com.nextbreakpoint.shop.common.model.DesignDocument;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LoadDesignResponse {
    private final UUID uuid;
    private final DesignDocument design;

    public LoadDesignResponse(UUID uuid, DesignDocument design) {
        this.uuid = Objects.requireNonNull(uuid);
        this.design = design;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Optional<DesignDocument> getDesign() {
        return Optional.ofNullable(design);
    }
}
