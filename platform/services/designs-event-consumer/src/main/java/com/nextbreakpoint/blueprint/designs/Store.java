package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Single;

import java.util.Optional;
import java.util.UUID;

public interface Store {
    Single<Void> appendMessage(UUID uuid, Message message);

    Single<Optional<Design>> updateDesign(UUID uuid);
}
