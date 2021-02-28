package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.PersistenceResult;
import com.nextbreakpoint.blueprint.common.core.command.DeleteDesign;
import com.nextbreakpoint.blueprint.common.core.command.InsertDesign;
import com.nextbreakpoint.blueprint.common.core.command.UpdateDesign;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import rx.Single;

public interface Store {
    Single<PersistenceResult> insertDesign(InsertDesign event);

    Single<PersistenceResult> updateDesign(UpdateDesign event);

    Single<PersistenceResult> deleteDesign(DeleteDesign event);

    Single<PersistenceResult> updateDesign(DesignChanged event);
}
