package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.model.GetStatusRequest;
import com.nextbreakpoint.shop.designs.model.GetStatusResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.ListStatusRequest;
import com.nextbreakpoint.shop.designs.model.ListStatusResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<DeleteDesignsResponse> deleteDesigns(DeleteDesignsRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);

    Single<GetStatusResponse> getStatus(GetStatusRequest request);

    Single<ListStatusResponse> listStatus(ListStatusRequest request);
}
