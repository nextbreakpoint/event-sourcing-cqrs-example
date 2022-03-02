package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocumentDeleteRequested {
    public static final String TYPE = "design-document-delete-requested-v1";

    private final UUID designId;
    private final String revision;

    @JsonCreator
    public DesignDocumentDeleteRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("revision") String revision
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.revision = Objects.requireNonNull(revision);
    }
}
