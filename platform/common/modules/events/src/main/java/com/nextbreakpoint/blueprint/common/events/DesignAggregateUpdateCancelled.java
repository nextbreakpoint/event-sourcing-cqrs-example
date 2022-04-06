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
        "revision",
        "checksum"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignAggregateUpdateCancelled {
    public static final String TYPE = "design-aggregate-update-cancelled-v1";

    private final UUID designId;
    private final String revision;
    private final String checksum;

    @JsonCreator
    public DesignAggregateUpdateCancelled(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("revision") String revision,
            @JsonProperty("checksum") String checksum
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.revision = Objects.requireNonNull(revision);
        this.checksum = Objects.requireNonNull(checksum);
    }
}
