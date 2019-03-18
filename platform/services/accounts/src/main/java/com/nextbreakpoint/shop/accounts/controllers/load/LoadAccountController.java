package com.nextbreakpoint.shop.accounts.controllers.load;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.common.model.Controller;
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
