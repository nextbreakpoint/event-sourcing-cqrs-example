package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.designs.model.*;
import rx.Single;

import java.util.UUID;

public interface Store {
    Single<PersistenceResult<DesignDocument>> selectDesign(UUID designUuid);

    Single<PersistenceResult<VersionDocument>> selectVersion(String checksum);

    Single<PersistenceResult<RenderDocument>> selectRender(UUID versionUuid, short level);

    Single<PersistenceResult<TileDocument>> selectTile(UUID versionUuid, short level, short x, short y);

    Single<PersistenceResult<Void>> insertVersion(DesignVersion version);

    Single<PersistenceResult<Void>> insertRender(DesignRender render);

    Single<PersistenceResult<Void>> insertTile(DesignTile tile);

    Single<PersistenceResult<Void>> publishVersion(UUID uuid, String checksum);

    Single<PersistenceResult<Void>> publishRender(UUID uuid, UUID version, short level);

    Single<PersistenceResult<Void>> publishTile(UUID uuid, UUID version, short level, short x, short y);
}
