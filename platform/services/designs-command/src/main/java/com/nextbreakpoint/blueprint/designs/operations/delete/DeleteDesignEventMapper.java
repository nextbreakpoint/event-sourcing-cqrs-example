package com.nextbreakpoint.blueprint.designs.operations.delete;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;

public class DeleteDesignEventMapper implements Mapper<DeleteDesignRequest, DesignDeleteRequested> {
    @Override
    public DesignDeleteRequested transform(DeleteDesignRequest request) {
        return DesignDeleteRequested.builder()
                .withUserId(request.getOwner())
                .withEventId(Uuids.timeBased())
                .withDesignId(request.getUuid())
                .withChangeId(request.getChange())
                .build();
    }
}
