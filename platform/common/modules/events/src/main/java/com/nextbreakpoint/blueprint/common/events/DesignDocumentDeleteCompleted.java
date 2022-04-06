package com.nextbreakpoint.blueprint.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@JsonPropertyOrder({
        "designId",
        "revision"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDocumentDeleteCompleted {
    public static final String TYPE = "design-document-delete-completed-v1";

    private final UUID designId;
    private final String revision;

    @JsonCreator
    public DesignDocumentDeleteCompleted(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("revision") String revision
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.revision = Objects.requireNonNull(revision);
    }
}
