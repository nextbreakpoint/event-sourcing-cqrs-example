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
public class DesignAggregateUpdateCompleted {
    public static final String TYPE = "design-aggregate-update-completed-v1";

    private final UUID eventId;
    private final UUID designId;
    private final long revision;
    private final String data;
    private final String checksum;
    private final int levels;
    private final String status;

    @JsonCreator
    public DesignAggregateUpdateCompleted(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("designId") UUID designId,
        @JsonProperty("revision") long revision,
        @JsonProperty("data") String data,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("levels") int levels,
        @JsonProperty("status") String status
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.designId = Objects.requireNonNull(designId);
        this.revision = revision;
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);
        this.levels = levels;
        this.status = status;
    }
}
