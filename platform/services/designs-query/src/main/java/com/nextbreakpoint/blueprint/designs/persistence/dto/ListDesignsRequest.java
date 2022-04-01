package com.nextbreakpoint.blueprint.designs.persistence.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class ListDesignsRequest {
    private final boolean draft;
    private final int from;
    private final int size;
}
