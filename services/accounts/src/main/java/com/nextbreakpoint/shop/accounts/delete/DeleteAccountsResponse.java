package com.nextbreakpoint.shop.accounts.delete;

import java.util.Objects;

public class DeleteAccountsResponse {
    private final Integer result;

    public DeleteAccountsResponse(Integer result) {
        this.result = Objects.requireNonNull(result);
    }

    public Integer getResult() {
        return result;
    }
}
