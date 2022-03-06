package com.nextbreakpoint.blueprint.designs.persistence.dto;

import com.nextbreakpoint.blueprint.designs.model.Design;

import java.util.List;
import java.util.Objects;

public class ListDesignsResponse {
    private final List<Design> designs;

    public ListDesignsResponse(List<Design> designs) {
        this.designs = Objects.requireNonNull(designs);
    }

    public List<Design> getDesigns() {
        return designs;
    }
}
