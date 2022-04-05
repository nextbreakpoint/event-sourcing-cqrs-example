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

    public static String getTopicName(String topicPrefix, int level) {
        if (level < 3) {
            return topicPrefix + "-0";
        } else if (level < 6) {
            return topicPrefix + "-1";
        } else {
            return topicPrefix + "-" + (level - 4);
        }
    }
}
