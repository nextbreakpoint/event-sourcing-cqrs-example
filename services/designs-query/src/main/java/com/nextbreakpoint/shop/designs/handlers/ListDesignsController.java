package com.nextbreakpoint.shop.designs.handlers;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import rx.Single;

import java.util.Objects;

public class ListDesignsController implements Controller<ListDesignsRequest, ListDesignsResponse> {
    private final Store store;

    public ListDesignsController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<ListDesignsResponse> onNext(ListDesignsRequest request) {
        return store.listDesigns(request);
    }
}
