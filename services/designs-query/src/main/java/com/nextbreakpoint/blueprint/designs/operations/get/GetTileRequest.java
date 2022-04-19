package com.nextbreakpoint.blueprint.designs.operations.get;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder(setterPrefix = "with")
public class GetTileRequest {
    private final UUID uuid;
    private final int level;
    private final int row;
    private final int col;
    private final int size;
    private final boolean draft;

    public GetTileRequest(UUID uuid, int level, int row, int col, int size, boolean draft) {
        this.uuid = Objects.requireNonNull(uuid);
        this.level = level;
        this.row = row;
        this.col = col;
        this.size = size;
        this.draft = draft;
    }
}
