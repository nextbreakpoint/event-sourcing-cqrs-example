package com.nextbreakpoint.blueprint.designs.operations.update;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;

public class UpdateDesignEventMapper implements Mapper<UpdateDesignRequest, DesignUpdateRequested> {
    @Override
    public DesignUpdateRequested transform(UpdateDesignRequest request) {
        return DesignUpdateRequested.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(request.getUuid())
                .withData(request.getJson())
                .withLevels(request.getLevels())
                .build();
    }
}
