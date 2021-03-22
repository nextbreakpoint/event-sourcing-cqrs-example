package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.CommandStatus;

import java.util.Objects;
import java.util.UUID;

public class UpdateDesignResponse {
    private final UUID uuid;
    private final CommandStatus status;

    public UpdateDesignResponse(UUID uuid, CommandStatus status) {
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
