package com.nextbreakpoint.blueprint.designs.operations.insert;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;

public class InsertDesignEventMapper implements Mapper<InsertDesignRequest, DesignInsertRequested> {
    @Override
    public DesignInsertRequested transform(InsertDesignRequest request) {
        return DesignInsertRequested.builder()
                .withEvid(Uuids.timeBased())
                .withUuid(request.getUuid())
                .withData(request.getJson())
                .withLevels(request.getLevels())
                .build();
    }
}
