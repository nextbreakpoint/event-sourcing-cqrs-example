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
        "userId",
        "data"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignInsertRequested {
    public static final String TYPE = "design-insert-requested-v1";

    private final UUID designId;
    private final UUID commandId;
    private final UUID userId;
    private final String data;

    @JsonCreator
    public DesignInsertRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("data") String data
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.commandId = Objects.requireNonNull(commandId);
        this.userId = Objects.requireNonNull(userId);
        this.data = Objects.requireNonNull(data);
    }
}
