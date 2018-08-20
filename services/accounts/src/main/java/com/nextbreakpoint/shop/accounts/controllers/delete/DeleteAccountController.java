package com.nextbreakpoint.shop.accounts.controllers.delete;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.common.model.Controller;
import rx.Single;

import java.util.Objects;

public class DeleteAccountController implements Controller<DeleteAccountRequest, DeleteAccountResponse> {
    private final Store store;

    public DeleteAccountController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<DeleteAccountResponse> onNext(DeleteAccountRequest request) {
        return store.deleteAccount(request);
    }
}
