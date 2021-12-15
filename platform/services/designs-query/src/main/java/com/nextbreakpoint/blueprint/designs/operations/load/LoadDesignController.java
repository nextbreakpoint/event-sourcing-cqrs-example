package com.nextbreakpoint.blueprint.designs.operations.load;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
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
