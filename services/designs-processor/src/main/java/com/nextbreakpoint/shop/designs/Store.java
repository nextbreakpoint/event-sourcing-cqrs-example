package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.InsertDesignEvent;
import com.nextbreakpoint.shop.common.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import rx.Single;

public interface Store {
    Single<InsertDesignResult> insertDesign(InsertDesignEvent request);

    Single<UpdateDesignResult> updateDesign(UpdateDesignEvent request);

    Single<DeleteDesignResult> deleteDesign(DeleteDesignEvent request);

    Single<DeleteDesignsResult> deleteDesigns(DeleteDesignsEvent request);
}
