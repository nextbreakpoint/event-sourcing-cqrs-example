package com.nextbreakpoint.blueprint.accounts.operations.delete;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class DeleteAccountRequest {
    private final UUID uuid;

    public DeleteAccountRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid);
    }
}
