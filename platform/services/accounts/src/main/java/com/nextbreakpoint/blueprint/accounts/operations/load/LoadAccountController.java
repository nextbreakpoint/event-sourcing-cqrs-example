package com.nextbreakpoint.blueprint.accounts.operations.load;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.common.core.Controller;
import rx.Single;

import java.util.Objects;

public class LoadAccountController implements Controller<LoadAccountRequest, LoadAccountResponse> {
    private final Store store;

    public LoadAccountController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<LoadAccountResponse> onNext(LoadAccountRequest request) {
        return store.loadAccount(request);
    }
}
