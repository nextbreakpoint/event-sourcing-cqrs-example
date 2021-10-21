package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.*;
import rx.Single;

import java.util.Optional;
import java.util.UUID;

public interface Store {
    Single<Void> insertDesign(UUID timeuuid, UUID uuid, String json);

    Single<Void> updateDesign(UUID timeuuid, UUID uuid, String json);

    Single<Void> deleteDesign(UUID timeuuid, UUID uuid);

    Single<Optional<DesignChange>> updateDesignAggregate(UUID uuid);

    Single<Void> insertDesignVersion(DesignVersion version);

    Single<Void> insertDesignTile(DesignTile tile);

    Single<Void> updateDesignTile(DesignTile tile, String status);
}
