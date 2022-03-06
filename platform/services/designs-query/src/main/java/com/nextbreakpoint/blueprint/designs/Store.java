package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.persistence.dto.*;
import rx.Single;

public interface Store {
    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);

    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);
}
