package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class InsertDesignCommandMapper implements Mapper<InsertDesignRequest, DesignInsertCommand> {
    @Override
    public DesignInsertCommand transform(InsertDesignRequest request) {
        return DesignInsertCommand.builder()
                .withUserId(request.getOwner())
                .withDesignId(request.getUuid())
                .withCommandId(request.getChange())
                .withData(request.getJson())
                .withLevels(request.getLevels())
                .build();
    }
}
