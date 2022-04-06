package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import rx.Single;

public interface Store {
    Single<Void> appendMessage(InputMessage message);

    Single<Boolean> existsTable(String tableName);
}
