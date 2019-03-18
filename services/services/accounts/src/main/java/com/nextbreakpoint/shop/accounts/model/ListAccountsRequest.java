package com.nextbreakpoint.shop.accounts.model;

import java.util.Optional;

public class ListAccountsRequest {
    private final String email;

    public ListAccountsRequest(String email) {
        this.email = email;
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }
}
