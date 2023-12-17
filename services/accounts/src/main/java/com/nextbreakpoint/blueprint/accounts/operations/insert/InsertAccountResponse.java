package com.nextbreakpoint.blueprint.accounts.operations.insert;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class InsertAccountResponse {
    private final UUID uuid;
    private final String authorities;
    private final Integer result;

    public InsertAccountResponse(UUID uuid, String authorities, Integer result) {
        this.uuid = Objects.requireNonNull(uuid);
        this.authorities = Objects.requireNonNull(authorities);
        this.result = Objects.requireNonNull(result);
    }
}
