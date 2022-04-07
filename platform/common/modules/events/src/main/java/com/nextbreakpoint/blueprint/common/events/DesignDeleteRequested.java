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
        "userId"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDeleteRequested {
    public static final String TYPE = "design-delete-requested-v1";

    private final UUID designId;
    private final UUID commandId;
    private final UUID userId;

    @JsonCreator
    public DesignDeleteRequested(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("userId") UUID userId
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.commandId = Objects.requireNonNull(commandId);
        this.userId = Objects.requireNonNull(userId);
    }
}
