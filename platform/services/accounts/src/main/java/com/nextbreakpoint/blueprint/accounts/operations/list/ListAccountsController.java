package com.nextbreakpoint.blueprint.accounts.operations.list;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import rx.Single;

import java.util.Objects;

public class ListAccountsController implements Controller<ListAccountsRequest, ListAccountsResponse> {
    private final Store store;

    public ListAccountsController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<ListAccountsResponse> onNext(ListAccountsRequest request) {
        return store.listAccounts(request);
    }
}
