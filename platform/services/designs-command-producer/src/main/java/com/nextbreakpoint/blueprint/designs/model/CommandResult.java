package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;
import java.util.UUID;

public class CommandResult {
    private final UUID uuid;
    private final CommandStatus status;

    public CommandResult(UUID uuid, CommandStatus status) {
        this.uuid = Objects.requireNonNull(uuid);
        this.status = Objects.requireNonNull(status);
    }

    public UUID getUuid() {
        return uuid;
    }

    public CommandStatus getStatus() {
        return status;
    }
}
