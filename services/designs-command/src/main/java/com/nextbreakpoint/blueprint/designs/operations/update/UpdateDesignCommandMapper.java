package com.nextbreakpoint.blueprint.designs.operations.update;

import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class UpdateDesignCommandMapper implements Mapper<UpdateDesignRequest, DesignUpdateCommand> {
    @Override
    public DesignUpdateCommand transform(UpdateDesignRequest request) {
        return DesignUpdateCommand.builder()
                .withUserId(request.getOwner())
                .withDesignId(request.getUuid())
                .withCommandId(request.getChange())
                .withData(request.getJson())
                .withPublished(request.getPublished())
                .build();
    }
}
