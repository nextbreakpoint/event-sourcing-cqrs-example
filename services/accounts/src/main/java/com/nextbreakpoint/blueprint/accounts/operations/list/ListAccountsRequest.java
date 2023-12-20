package com.nextbreakpoint.blueprint.accounts.operations.list;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(setterPrefix = "with")
public class ListAccountsRequest {
    private final String email;

    public ListAccountsRequest(String email) {
        this.email = email;
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }
}
