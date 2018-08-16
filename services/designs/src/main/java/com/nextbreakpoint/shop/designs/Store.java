package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.designs.handlers.delete.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.handlers.delete.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.handlers.delete.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.handlers.delete.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.get.GetStatusRequest;
import com.nextbreakpoint.shop.designs.get.GetStatusResponse;
import com.nextbreakpoint.shop.designs.handlers.insert.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.handlers.insert.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.list.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.list.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.list.ListStatusRequest;
import com.nextbreakpoint.shop.designs.list.ListStatusResponse;
import com.nextbreakpoint.shop.designs.load.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.load.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.handlers.update.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.handlers.update.UpdateDesignResponse;
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
