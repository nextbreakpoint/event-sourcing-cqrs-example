package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Store {
    Single<List<InputMessage>> findMessages(UUID uuid, String fromRevision, String toRevision);

    Single<Void> appendMessage(InputMessage message);

    Single<Void> updateDesign(Design design);

    Single<Void> deleteDesign(Design design);

    Single<Optional<Design>> findDesign(UUID uuid);

    Single<Boolean> existsTable(String designs);
}
