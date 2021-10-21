package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.DesignChange;
import com.nextbreakpoint.blueprint.designs.model.PersistenceResult;
import rx.Single;

import java.util.UUID;

public interface Store {
    Single<PersistenceResult<Void>> insertDesign(UUID uuid, UUID eventTimestamp, String json);

    Single<PersistenceResult<Void>> updateDesign(UUID uuid, UUID eventTimestamp, String json);

    Single<PersistenceResult<Void>> deleteDesign(UUID uuid, UUID eventTimestamp);

    Single<PersistenceResult<DesignChange>> updateDesignAggregate(UUID uuid, UUID eventTimestamp);

    Single<PersistenceResult<Void>> publishDesign(UUID uuid, UUID eventTimestamp);
}
