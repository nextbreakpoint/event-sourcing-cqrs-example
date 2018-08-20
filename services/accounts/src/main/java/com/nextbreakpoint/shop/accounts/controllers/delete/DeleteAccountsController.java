package com.nextbreakpoint.shop.accounts.controllers.delete;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountsRequest;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountsResponse;
import com.nextbreakpoint.shop.common.model.Controller;
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
