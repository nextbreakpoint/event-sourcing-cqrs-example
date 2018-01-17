package com.nextbreakpoint.shop.accounts.insert;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.common.Controller;
import rx.Single;

import java.util.Objects;

public class InsertAccountController implements Controller<InsertAccountRequest, InsertAccountResponse> {
    private final Store store;

    public InsertAccountController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<InsertAccountResponse> apply(InsertAccountRequest request) {
        return store.insertAccount(request);
    }
}
