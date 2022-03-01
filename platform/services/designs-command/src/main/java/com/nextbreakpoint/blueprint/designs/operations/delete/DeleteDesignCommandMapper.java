package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.TimeUUID;

public class DeleteDesignCommandMapper implements Mapper<DeleteDesignRequest, DesignDeleteCommand> {
    @Override
    public DesignDeleteCommand transform(DeleteDesignRequest request) {
        return DesignDeleteCommand.builder()
                .withUserId(request.getOwner())
                .withEventId(TimeUUID.next())
                .withDesignId(request.getUuid())
                .withChangeId(request.getChange())
                .build();
    }
}
