package com.nextbreakpoint.blueprint.designs.persistence.dto;

import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(setterPrefix = "with")
public class LoadDesignResponse {
    private final Design design;

    public LoadDesignResponse(Design design) {
        this.design = design;
    }

    public Optional<Design> getDesign() {
        return Optional.ofNullable(design);
    }
}
