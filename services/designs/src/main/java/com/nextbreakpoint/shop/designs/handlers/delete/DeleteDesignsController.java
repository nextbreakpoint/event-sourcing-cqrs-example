package com.nextbreakpoint.shop.designs.handlers.delete;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.designs.Store;
import rx.Single;

import java.util.Objects;

public class DeleteDesignsController implements Controller<DeleteDesignsRequest, DeleteDesignsResponse> {
    private final Store store;

    public DeleteDesignsController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<DeleteDesignsResponse> onNext(DeleteDesignsRequest request) {
        return store.deleteDesigns(request);
    }
}
