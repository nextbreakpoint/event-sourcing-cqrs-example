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
        "commandId",
        "revision"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignAggregateTilesUpdateRequested {
    public static final String TYPE = "design-aggregate-tiles-update-requested-v1";

    private final UUID designId;
    private final UUID commandId;
    private final String revision;

    @JsonCreator
    public DesignAggregateTilesUpdateRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("revision") String revision
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.commandId = Objects.requireNonNull(commandId);
        this.revision = Objects.requireNonNull(revision);
    }
}
