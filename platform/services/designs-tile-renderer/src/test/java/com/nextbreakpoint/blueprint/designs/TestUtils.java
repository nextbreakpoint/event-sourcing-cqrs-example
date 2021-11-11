package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import org.jetbrains.annotations.NotNull;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }
}
