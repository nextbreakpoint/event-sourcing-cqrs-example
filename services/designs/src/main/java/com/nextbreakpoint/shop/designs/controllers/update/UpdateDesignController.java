package com.nextbreakpoint.shop.designs.controllers.update;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import rx.Single;

import java.util.Objects;

public class UpdateDesignController implements Controller<UpdateDesignRequest, UpdateDesignResponse> {
    private final Store store;

    public UpdateDesignController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<UpdateDesignResponse> onNext(UpdateDesignRequest request) {
        return store.updateDesign(request);
    }
}
