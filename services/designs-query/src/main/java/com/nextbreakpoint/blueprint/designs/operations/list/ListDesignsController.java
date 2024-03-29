package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;
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
