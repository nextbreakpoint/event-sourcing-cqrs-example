package com.nextbreakpoint.blueprint.common.commands;

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
        "userId",
        "commandId",
        "data"
})
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignInsertCommand {
    public static final String TYPE = "design-insert-command-v1";

    private final UUID designId;
    private final UUID userId;
    private final UUID commandId;
    private final String data;

    @JsonCreator
    public DesignInsertCommand(
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
