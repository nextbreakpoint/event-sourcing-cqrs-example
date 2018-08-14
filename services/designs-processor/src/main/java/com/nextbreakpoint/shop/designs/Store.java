package com.nextbreakpoint.shop.designs;

import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<DeleteDesignsResponse> deleteDesigns(DeleteDesignsRequest request);
}
