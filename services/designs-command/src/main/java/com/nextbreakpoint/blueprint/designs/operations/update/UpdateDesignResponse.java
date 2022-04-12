package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.core.ResultStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class UpdateDesignResponse {
    private final UUID uuid;
    private final ResultStatus status;
    private final String error;

    public UpdateDesignResponse(UUID uuid, ResultStatus status, String error) {
        this.uuid = Objects.requireNonNull(uuid);
        this.status = Objects.requireNonNull(status);
        this.error = error;
    }

    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }
}
