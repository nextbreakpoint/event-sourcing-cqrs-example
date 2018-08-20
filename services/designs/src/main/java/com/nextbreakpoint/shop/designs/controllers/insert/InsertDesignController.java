package com.nextbreakpoint.shop.designs.controllers.insert;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import rx.Single;

import java.util.Objects;

public class InsertDesignController implements Controller<InsertDesignRequest, InsertDesignResponse> {
    private final Store store;

    public InsertDesignController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<InsertDesignResponse> onNext(InsertDesignRequest request) {
        return store.insertDesign(request);
    }
}
