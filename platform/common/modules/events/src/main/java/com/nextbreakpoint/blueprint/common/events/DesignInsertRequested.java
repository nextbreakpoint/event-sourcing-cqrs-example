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
public class DesignInsertRequested {
    public static final String TYPE = "design-insert-requested-v1";

    private final UUID designId;
    private final UUID userId;
    private final UUID commandId;
    private final String data;

    @JsonCreator
    public DesignInsertRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("data") String data
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.userId = Objects.requireNonNull(userId);
        this.commandId = Objects.requireNonNull(commandId);
        this.data = Objects.requireNonNull(data);
    }
}
