package com.nextbreakpoint.blueprint.common.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class DesignDeleteCommand {
    public static final String TYPE = "design-delete-command-v1";

    private final UUID userId;
    private final UUID designId;
    private final UUID commandId;

    @JsonCreator
    public DesignDeleteCommand(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("commandId") UUID commandId
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.userId = Objects.requireNonNull(userId);
        this.commandId = Objects.requireNonNull(commandId);
    }
}
