package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.core.CommandStatus;

import java.util.Objects;
import java.util.UUID;

public class DeleteDesignResponse {
    private final UUID uuid;
    private final CommandStatus status;

    public DeleteDesignResponse(UUID uuid, CommandStatus status) {
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
