package com.nextbreakpoint.blueprint.designs.persistence;

import com.nextbreakpoint.blueprint.designs.model.Design;

import java.util.Optional;

public class LoadDesignResponse {
    private final Design design;

    public LoadDesignResponse(Design design) {
        this.design = design;
    }

    public Optional<Design> getDesign() {
        return Optional.ofNullable(design);
    }
}
