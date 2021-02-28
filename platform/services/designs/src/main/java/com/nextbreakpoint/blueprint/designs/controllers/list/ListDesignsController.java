package com.nextbreakpoint.blueprint.designs.controllers.list;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsResponse;
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
