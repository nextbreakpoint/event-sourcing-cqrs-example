package com.nextbreakpoint.blueprint.accounts.operations.load;

import com.nextbreakpoint.blueprint.accounts.model.Account;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LoadAccountResponse {
    private final UUID uuid;
    private final Account account;

    public LoadAccountResponse(UUID uuid, Account account) {
        this.uuid = Objects.requireNonNull(uuid);
        this.account = account;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Optional<Account> getAccount() {
        return Optional.ofNullable(account);
    }
}
