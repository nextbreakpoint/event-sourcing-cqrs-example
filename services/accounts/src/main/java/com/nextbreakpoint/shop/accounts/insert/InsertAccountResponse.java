package com.nextbreakpoint.shop.accounts.insert;

import java.util.Objects;
import java.util.UUID;

public class InsertAccountResponse {
    private final UUID uuid;
    private final Integer result;

    public InsertAccountResponse(UUID uuid, Integer result) {
        this.uuid = Objects.requireNonNull(uuid);
        this.result = Objects.requireNonNull(result);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getResult() {
        return result;
    }
}
