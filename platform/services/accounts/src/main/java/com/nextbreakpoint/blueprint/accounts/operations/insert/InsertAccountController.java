package com.nextbreakpoint.blueprint.accounts.operations.insert;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
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
