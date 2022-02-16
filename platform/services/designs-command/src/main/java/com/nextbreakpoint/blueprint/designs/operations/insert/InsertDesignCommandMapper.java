package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class InsertDesignCommandMapper implements Mapper<InsertDesignRequest, DesignInsertCommand> {
    @Override
    public DesignInsertCommand transform(InsertDesignRequest request) {
        return DesignInsertCommand.builder()
                .withUserId(request.getOwner())
                .withEventId(Uuids.timeBased())
                .withDesignId(request.getUuid())
                .withChangeId(request.getChange())
                .withData(request.getJson())
                .withLevels(request.getLevels())
                .build();
    }
}
