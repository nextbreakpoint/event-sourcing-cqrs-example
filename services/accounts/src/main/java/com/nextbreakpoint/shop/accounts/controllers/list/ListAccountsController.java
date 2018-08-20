package com.nextbreakpoint.shop.accounts.controllers.list;

import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.shop.common.model.Controller;
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
