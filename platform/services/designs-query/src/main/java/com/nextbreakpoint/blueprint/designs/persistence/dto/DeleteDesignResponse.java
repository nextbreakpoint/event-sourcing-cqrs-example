package com.nextbreakpoint.blueprint.designs.persistence.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class DeleteDesignResponse {
    public DeleteDesignResponse() {}
}
