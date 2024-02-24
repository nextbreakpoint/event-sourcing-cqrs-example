package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;

public class Bucket {
    private Bucket() {}

    public static String createBucketKey(TileRenderRequested event) {
        return "tiles/%s/%d/%04d%04d.png".formatted(event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static String createCacheKey(String checksum) {
        return "cache/%s.png".formatted(checksum);
    }
}
