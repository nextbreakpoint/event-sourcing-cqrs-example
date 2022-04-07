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
        "revision",
        "checksum",
        "data",
        "levels",
        "status"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignAggregateUpdateCompleted {
    public static final String TYPE = "design-aggregate-update-completed-v1";

    private final UUID designId;
    private final UUID commandId;
    private final String revision;
    private final String checksum;
    private final String data;
    private final int levels;
    private final String status;

    @JsonCreator
    public DesignAggregateUpdateCompleted(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("revision") String revision,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("data") String data,
            @JsonProperty("levels") int levels,
            @JsonProperty("status") String status
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.commandId = Objects.requireNonNull(commandId);
        this.revision = Objects.requireNonNull(revision);
        this.checksum = Objects.requireNonNull(checksum);
        this.data = Objects.requireNonNull(data);
        this.levels = levels;
        this.status = status;
    }
}
