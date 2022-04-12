package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Mapper;

public class DeleteDesignCommandMapper implements Mapper<DeleteDesignRequest, DesignDeleteCommand> {
    @Override
    public DesignDeleteCommand transform(DeleteDesignRequest request) {
        return DesignDeleteCommand.builder()
                .withUserId(request.getOwner())
                .withDesignId(request.getUuid())
                .withCommandId(request.getChange())
                .build();
    }
}
