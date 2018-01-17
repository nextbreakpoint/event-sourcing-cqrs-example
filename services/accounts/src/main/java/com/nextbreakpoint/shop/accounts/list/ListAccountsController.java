package com.nextbreakpoint.shop.accounts.list;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.common.Controller;
import rx.Single;

import java.util.Objects;

public class ListAccountsController implements Controller<ListAccountsRequest, ListAccountsResponse> {
    private final Store store;

    public ListAccountsController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<ListAccountsResponse> apply(ListAccountsRequest request) {
        return store.listAccounts(request);
    }
}
