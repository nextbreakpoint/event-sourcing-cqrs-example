package com.nextbreakpoint.blueprint.accounts.operations.load;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class LoadAccountRequest {
    private final UUID uuid;

    public LoadAccountRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }
}
