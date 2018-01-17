package com.nextbreakpoint.shop.designs.load;

import com.nextbreakpoint.shop.designs.model.Design;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LoadDesignResponse {
    private final UUID uuid;
    private final Design design;

    public LoadDesignResponse(UUID uuid, Design design) {
        this.uuid = Objects.requireNonNull(uuid);
        this.design = design;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Optional<Design> getDesign() {
        return Optional.ofNullable(design);
    }
}
