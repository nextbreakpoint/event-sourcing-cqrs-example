package com.nextbreakpoint.shop.designs.load;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.designs.Store;
import rx.Single;

import java.util.Objects;

public class LoadDesignController implements Controller<LoadDesignRequest, LoadDesignResponse> {
    private final Store store;

    public LoadDesignController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<LoadDesignResponse> onNext(LoadDesignRequest request) {
        return store.loadDesign(request);
    }
}
