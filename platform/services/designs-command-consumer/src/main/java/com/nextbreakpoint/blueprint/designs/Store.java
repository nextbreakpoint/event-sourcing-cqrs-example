package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.DesignChange;
import com.nextbreakpoint.blueprint.designs.model.PersistenceResult;
import rx.Single;

import java.util.UUID;

public interface Store {
    Single<PersistenceResult<Void>> appendInsertDesignEvent(UUID uuid, UUID eventTimestamp, String json);

    Single<PersistenceResult<Void>> appendUpdateDesignEvent(UUID uuid, UUID eventTimestamp, String json);

    Single<PersistenceResult<Void>> appendDeleteDesignEvent(UUID uuid, UUID eventTimestamp);

    Single<PersistenceResult<DesignChange>> updateAggregate(UUID uuid, UUID eventTimestamp);

    Single<PersistenceResult<Void>> publishEvent(UUID uuid, UUID eventTimestamp);
}
