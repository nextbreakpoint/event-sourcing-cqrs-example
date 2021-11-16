package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.ResultStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UpdateDesignResponse {
    private final UUID uuid;
    private final ResultStatus status;
    private final String error;

    public UpdateDesignResponse(UUID uuid, ResultStatus status) {
        this(uuid, status, null);
    }

    public UpdateDesignResponse(UUID uuid, ResultStatus status, String error) {
        this.uuid = Objects.requireNonNull(uuid);
        this.status = Objects.requireNonNull(status);
        this.error = error;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }
}
