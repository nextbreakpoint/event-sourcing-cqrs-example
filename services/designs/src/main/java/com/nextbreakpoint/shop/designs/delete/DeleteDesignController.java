package com.nextbreakpoint.shop.designs.delete;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.designs.Store;
import rx.Single;

import java.util.Objects;

public class DeleteDesignController implements Controller<DeleteDesignRequest, DeleteDesignResponse> {
    private final Store store;

    public DeleteDesignController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<DeleteDesignResponse> apply(DeleteDesignRequest request) {
        return store.deleteDesign(request);
    }
}
