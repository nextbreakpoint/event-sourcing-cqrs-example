package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import rx.Single;

public interface Store {
    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);

    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<Boolean> existsIndex(String indexName);
}
