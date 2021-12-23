package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;

public class Bucket {
    private Bucket() {}

    public static String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static String createBucketKey(TileRenderAborted event) {
        return String.format("%s/%d/%04d%04d.png", event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }
}
