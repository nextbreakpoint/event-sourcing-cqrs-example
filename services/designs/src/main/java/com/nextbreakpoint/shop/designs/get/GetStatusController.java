package com.nextbreakpoint.shop.designs.get;

import com.nextbreakpoint.shop.common.Controller;
import com.nextbreakpoint.shop.designs.Store;
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
