package com.nextbreakpoint.blueprint.accounts.operations.list;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(setterPrefix = "with")
public class ListAccountsRequest {
    private final String login;

    public ListAccountsRequest(String login) {
        this.login = login;
    }

    public Optional<String> getLogin() {
        return Optional.ofNullable(login);
    }
}
