package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.Optional;
import java.util.UUID;

public interface Store {
    Single<Void> appendMessage(InputMessage message);

    Single<Optional<Design>> updateDesign(UUID uuid, long esid);

    Single<Optional<Design>> findDesign(UUID uuid);
}
