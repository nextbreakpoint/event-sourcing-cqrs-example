package com.nextbreakpoint.shop.designs.controllers.list;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.ListStatusRequest;
import com.nextbreakpoint.shop.designs.model.ListStatusResponse;
import rx.Single;

import java.util.Objects;

public class ListStatusController implements Controller<ListStatusRequest, ListStatusResponse> {
    private final Store store;

    public ListStatusController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<ListStatusResponse> onNext(ListStatusRequest request) {
        return store.listStatus(request);
    }
}
