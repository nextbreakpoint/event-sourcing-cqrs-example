package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.blueprint.designs.model.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.InsertDesignResponse;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.model.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.LoadDesignResponse;
import com.nextbreakpoint.blueprint.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.blueprint.designs.model.UpdateDesignResponse;
import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);
}
