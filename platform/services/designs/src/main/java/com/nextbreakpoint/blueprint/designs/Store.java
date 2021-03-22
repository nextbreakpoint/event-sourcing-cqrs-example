package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.delete.DeleteDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.insert.InsertDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponse;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.update.UpdateDesignResponse;
import rx.Single;

public interface Store {
    Single<InsertDesignResponse> insertDesign(InsertDesignRequest request);

    Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request);

    Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request);

    Single<LoadDesignResponse> loadDesign(LoadDesignRequest request);

    Single<ListDesignsResponse> listDesigns(ListDesignsRequest request);
}
