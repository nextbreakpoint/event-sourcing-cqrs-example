package com.nextbreakpoint.blueprint.designs.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocuments {
    private final List<DesignDocument> designs;
    private final long total;
}
