package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import rx.Single;

import java.util.List;
import java.util.UUID;

public interface Store {
    Single<List<InputMessage>> findMessages(UUID uuid, String fromRevision, String toRevision);

    Single<Void> appendMessage(InputMessage message);

    Single<Boolean> existsTable(String tableName);
}
