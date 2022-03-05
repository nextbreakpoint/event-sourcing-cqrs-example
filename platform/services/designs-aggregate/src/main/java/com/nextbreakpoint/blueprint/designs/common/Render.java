package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.TileRenderAborted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;

public class Render {
    private Render() {}

    public static String createRenderKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getDesignId(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static String createRenderKey(TileRenderAborted event) {
        return String.format("%s/%d/%04d%04d.png", event.getDesignId(), event.getLevel(), event.getRow(), event.getCol());
    }
}
