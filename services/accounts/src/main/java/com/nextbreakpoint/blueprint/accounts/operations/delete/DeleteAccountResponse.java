package com.nextbreakpoint.blueprint.accounts.operations.delete;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class DeleteAccountResponse {
    private final UUID uuid;
    private final Integer result;

    public DeleteAccountResponse(UUID uuid, Integer result) {
        this.uuid = Objects.requireNonNull(uuid);
        this.result = Objects.requireNonNull(result);
    }
}
