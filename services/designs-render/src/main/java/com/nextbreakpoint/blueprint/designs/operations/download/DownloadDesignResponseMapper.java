package com.nextbreakpoint.blueprint.designs.operations.download;

import com.nextbreakpoint.blueprint.common.core.Mapper;

public class DownloadDesignResponseMapper implements Mapper<DownloadDesignResponse, byte[]> {
    @Override
    public byte[] transform(DownloadDesignResponse response) {
        return response.getBytes().orElse(new byte[0]);
    }
}
