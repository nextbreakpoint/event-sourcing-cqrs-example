package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignResponse;
import rx.Single;

public interface Store {
    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);
}
