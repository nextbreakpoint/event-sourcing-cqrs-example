package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class Render {
    private Render() {}

    public static String createRenderKey(TileRenderRequested event) {
        return String.format("%s/%s/%d/%04d%04d.png", event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static String createRenderKey(TileRenderCompleted event) {
        return String.format("%s/%s/%d/%04d%04d.png", event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
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

    public static List<Tile> generateTiles(Tile tile, int levels, Bitmap bitmap) {
        int level = tile.getLevel();

        if (levels < 8 || level < 2) {
            return List.of();
        }

        if (level == 2) {
            int row = tile.getRow() * 2;
            int col = tile.getCol() * 2;

            return List.of(new Tile(level + 1, row, col));
        }

        if (level < levels) {
            Tile nextTile = generateTiles(tile);

            while (nextTile != null && bitmap.hasTile(nextTile.getLevel(), nextTile.getRow(), nextTile.getCol())) {
                log.debug("Skipping tile {}", nextTile);
                nextTile = generateTiles(nextTile);
            }

            if (nextTile != null) {
                return List.of(nextTile);
            }
        }

        return List.of();
    }

    private static Tile generateTiles(Tile tile) {
        int level = tile.getLevel();

        int size = (int) Math.rint(Math.pow(2, level));

        int span = size / 4;

        int blockRow = tile.getRow() / span;
        int blockCol = tile.getCol() / span;

        int row = tile.getRow() % span;
        int col = tile.getCol() % span;

        if (row < span - 1) {
            if (col < span - 1) {
                col += 1;
            } else {
                col = 0;
                row += 1;
            }
        } else {
            if (col < span - 1) {
                col += 1;
            } else {
                col = 0;
                row = 0;

                level += 1;

                span *= 2;
            }
        }

        if (level < 8) {
            return new Tile(level, blockRow * span + row, blockCol * span + col);
        }

        return null;
    }
}
