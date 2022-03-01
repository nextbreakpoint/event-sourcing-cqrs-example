package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.TimeUUID;

public class InsertDesignCommandMapper implements Mapper<InsertDesignRequest, DesignInsertCommand> {
    @Override
    public DesignInsertCommand transform(InsertDesignRequest request) {
        return DesignInsertCommand.builder()
                .withUserId(request.getOwner())
                .withEventId(TimeUUID.next())
                .withDesignId(request.getUuid())
                .withChangeId(request.getChange())
                .withData(request.getJson())
                .withLevels(request.getLevels())
                .build();
    }
}
