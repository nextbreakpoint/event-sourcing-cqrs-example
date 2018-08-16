package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.common.Controller;
import rx.Single;

import java.util.Objects;

public class DeleteAccountsController implements Controller<DeleteAccountsRequest, DeleteAccountsResponse> {
    private final Store store;

    public DeleteAccountsController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<DeleteAccountsResponse> onNext(DeleteAccountsRequest request) {
        return store.deleteAccounts(request);
    }
}
