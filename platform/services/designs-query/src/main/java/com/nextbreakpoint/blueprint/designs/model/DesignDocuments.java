package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocuments {
    private final List<DesignDocument> designs;
    private final long total;

    @JsonCreator
    public DesignDocuments(
        @JsonProperty("designs") List<DesignDocument> designs,
        @JsonProperty("total") long total
    ) {
        this.designs = designs;
        this.total = total;
    }
}
