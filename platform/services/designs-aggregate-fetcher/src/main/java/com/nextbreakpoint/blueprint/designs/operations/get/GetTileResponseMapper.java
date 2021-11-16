package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Mapper;

import java.util.Optional;

public class GetTileResponseMapper implements Mapper<GetTileResponse, Optional<byte[]>> {
    @Override
    public Optional<byte[]> transform(GetTileResponse response) {
        return response.getData();
    }
}
