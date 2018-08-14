package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.designs.list.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.list.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.load.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.load.LoadDesignResponse;
import rx.Single;

public interface Store {
    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);
}
