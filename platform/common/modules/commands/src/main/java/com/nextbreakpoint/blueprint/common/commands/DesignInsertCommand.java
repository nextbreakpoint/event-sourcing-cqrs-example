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
public class DesignInsertCommand {
    public static final String TYPE = "design-insert-command-v1";

    private final UUID userId;
    private final UUID designId;
    private final UUID commandId;
    private final String data;
    private final int levels;

    @JsonCreator
    public DesignInsertCommand(
            @JsonProperty("designId") UUID designId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("commandId") UUID commandId,
            @JsonProperty("data") String data,
            @JsonProperty("levels") int levels
    ) {
        this.designId = Objects.requireNonNull(designId);
        this.userId = Objects.requireNonNull(userId);
        this.commandId = Objects.requireNonNull(commandId);
        this.data = Objects.requireNonNull(data);
        this.levels = levels;
    }
}
