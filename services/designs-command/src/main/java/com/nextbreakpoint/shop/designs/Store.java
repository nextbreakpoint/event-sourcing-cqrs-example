package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponse;
import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<DeleteDesignsResponse> deleteDesigns(DeleteDesignsRequest request);
}
