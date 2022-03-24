package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;

public class Render {
    private Render() {}

    public static String createRenderKey(TileRenderCompleted event) {
        return String.format("%s/%d/%04d%04d.png", event.getDesignId(), event.getLevel(), event.getRow(), event.getCol());
    }
}
