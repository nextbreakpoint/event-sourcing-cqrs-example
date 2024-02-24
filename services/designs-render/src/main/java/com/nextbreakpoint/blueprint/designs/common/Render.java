package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;

public class Render {
    private Render() {}

    public static String createRenderKey(TileRenderRequested event) {
        return "%s/%s/%d/%04d%04d".formatted(event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static String createRenderKey(TileRenderCompleted event) {
        return "%s/%s/%d/%04d%04d".formatted(event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
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
