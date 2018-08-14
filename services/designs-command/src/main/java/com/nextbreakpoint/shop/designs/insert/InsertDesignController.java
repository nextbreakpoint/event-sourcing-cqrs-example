package com.nextbreakpoint.shop.designs.insert;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.designs.Store;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private final Store store;

    public InsertDesignController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<InsertDesignResponse> apply(InsertDesignRequest request) {
        return store.insertDesign(request);
    }
}
