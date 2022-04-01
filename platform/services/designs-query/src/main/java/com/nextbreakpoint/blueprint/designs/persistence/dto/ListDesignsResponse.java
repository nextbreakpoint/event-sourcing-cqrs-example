package com.nextbreakpoint.blueprint.designs.persistence.dto;

import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class ListDesignsResponse {
    private final List<Design> designs;
    private final long total;

    public ListDesignsResponse(List<Design> designs, long total) {
        this.designs = Objects.requireNonNull(designs);
        this.total = total;
    }
}
