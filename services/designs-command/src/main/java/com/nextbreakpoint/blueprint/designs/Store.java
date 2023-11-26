package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import org.apache.avro.specific.SpecificRecord;
import rx.Single;

import java.util.List;
import java.util.UUID;

public interface Store {
    Single<List<InputMessage<SpecificRecord>>> findMessages(UUID uuid, String fromRevision, String toRevision);

    Single<Void> appendMessage(InputMessage<? extends SpecificRecord> message);

    Single<Boolean> existsTable(String tableName);
}
