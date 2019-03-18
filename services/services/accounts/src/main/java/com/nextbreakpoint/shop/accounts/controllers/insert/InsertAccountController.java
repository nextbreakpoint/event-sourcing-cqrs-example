package com.nextbreakpoint.shop.accounts.controllers.insert;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.common.model.Controller;
import rx.Single;

import java.util.Objects;

public class InsertAccountController implements Controller<InsertAccountRequest, InsertAccountResponse> {
    private final Store store;

    public InsertAccountController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<InsertAccountResponse> onNext(InsertAccountRequest request) {
        return store.insertAccount(request);
    }
}
