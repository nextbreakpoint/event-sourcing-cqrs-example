package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<DeleteDesignsResponse> deleteDesigns(DeleteDesignsRequest request);
}
