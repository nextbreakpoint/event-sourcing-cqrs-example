package com.nextbreakpoint.blueprint.common.core;

public interface MessageType {
    String DESIGN_INSERT_REQUESTED = "design-insert-requested";
    String DESIGN_UPDATE_REQUESTED = "design-update-requested";
    String DESIGN_DELETE_REQUESTED = "design-delete-requested";
    String AGGREGATE_UPDATE_REQUESTED = "aggregate-update-requested";
    String AGGREGATE_UPDATE_COMPLETED = "aggregate-update-completed";
    String TILE_RENDER_REQUESTED = "tile-render-requested";
    String TILE_RENDER_COMPLETED = "tile-render-completed";
    String TILE_RENDER_UPDATED = "tile-render-updated";
    String DESIGN_CHANGED = "design-changed";
}
