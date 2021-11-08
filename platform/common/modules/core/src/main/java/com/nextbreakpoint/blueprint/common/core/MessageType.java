package com.nextbreakpoint.blueprint.common.core;

public interface MessageType {
    String DESIGN_INSERT_REQUESTED = "design-insert-requested";
    String DESIGN_UPDATE_REQUESTED = "design-update-requested";
    String DESIGN_DELETE_REQUESTED = "design-delete-requested";
    String DESIGN_ABORT_REQUESTED = "design-abort-requested";
    String DESIGN_AGGREGATE_UPDATE_REQUESTED = "design-aggregate-update-requested";
    String DESIGN_AGGREGATE_UPDATE_COMPLETED = "design-aggregate-update-completed";
    String TILE_RENDER_REQUESTED = "tile-render-requested";
    String TILE_RENDER_COMPLETED = "tile-render-completed";
    String TILE_RENDER_ABORTED = "tile-render-aborted";
    String TILE_AGGREGATE_UPDATE_REQUIRED = "tile-aggregate-update-required";
    String TILE_AGGREGATE_UPDATE_REQUESTED = "tile-aggregate-update-requested";
    String TILE_AGGREGATE_UPDATE_COMPLETED = "tile-aggregate-update-completed";
    String DESIGN_CHANGED = "design-changed";
}
