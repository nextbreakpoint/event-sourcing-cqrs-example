package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.designs.model.PersistenceResult;
import rx.Single;

public interface Store {
    Single<PersistenceResult> insertDesign(InsertDesignCommand event);

    Single<PersistenceResult> updateDesign(UpdateDesignCommand event);

    Single<PersistenceResult> deleteDesign(DeleteDesignCommand event);

    Single<PersistenceResult> updateDesign(DesignChangedEvent event);
}
