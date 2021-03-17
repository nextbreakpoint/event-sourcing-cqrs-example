package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.*;

import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);
}
