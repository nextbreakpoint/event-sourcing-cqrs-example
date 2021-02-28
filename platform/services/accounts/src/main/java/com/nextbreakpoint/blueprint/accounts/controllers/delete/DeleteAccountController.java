package com.nextbreakpoint.blueprint.accounts.controllers.delete;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
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
