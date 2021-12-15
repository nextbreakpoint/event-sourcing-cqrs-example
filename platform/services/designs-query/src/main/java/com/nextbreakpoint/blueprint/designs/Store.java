package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponse;
import rx.Single;

public interface Store {
    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);
}
