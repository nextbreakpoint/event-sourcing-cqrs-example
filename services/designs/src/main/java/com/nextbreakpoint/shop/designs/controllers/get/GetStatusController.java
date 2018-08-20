package com.nextbreakpoint.shop.designs.controllers.get;

import com.nextbreakpoint.shop.common.model.Controller;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.GetStatusRequest;
import com.nextbreakpoint.shop.designs.model.GetStatusResponse;
import rx.Single;

import java.util.Objects;

public class GetStatusController implements Controller<GetStatusRequest, GetStatusResponse> {
    private final Store store;

    public GetStatusController(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Single<GetStatusResponse> onNext(GetStatusRequest request) {
        return store.getStatus(request);
    }
}
