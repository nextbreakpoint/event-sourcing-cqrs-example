package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import rx.Single;

public interface Store {
    Single<InsertDesignResult> insertDesign(InsertDesignEvent event);

    Single<UpdateDesignResult> updateDesign(UpdateDesignEvent event);

    Single<DeleteDesignResult> deleteDesign(DeleteDesignEvent event);

    Single<DeleteDesignsResult> deleteDesigns(DeleteDesignsEvent event);
}
