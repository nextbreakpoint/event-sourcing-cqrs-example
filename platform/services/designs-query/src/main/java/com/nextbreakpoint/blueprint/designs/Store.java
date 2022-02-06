package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.persistence.*;
import rx.Single;

public interface Store {
    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);

    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);
}
