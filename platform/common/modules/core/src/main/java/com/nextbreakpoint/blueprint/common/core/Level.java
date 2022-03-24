package com.nextbreakpoint.blueprint.common.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class Level {
    private int level;
    private List<Byte> tiles;

    private static List<Integer> COUNT_TABLE = IntStream.range(0, 256)
            .mapToObj(Level::countBits)
            .collect(Collectors.toList());

    private static int countBits(int bitmap) {
        return IntStream.range(0, 8)
                .map(shift -> bitmap & (1 << shift))
                .map(value -> value != 0 ? 1 : 0)
                .sum();
    }

    private Level(int level, List<Byte> tiles) {
        this.level = level;
        this.tiles = tiles;
    }

    public static Level createEmpty(int level) {
        return new Level(level, createTiles(level));
    }

    public static Level of(Integer level, List<Byte> tiles) {
        return new Level(level, tiles);
    }

    private static List<Byte> createTiles(int level) {
        switch (level) {
            case 0:
            case 1: {
                return new ArrayList<>(List.of((byte) 0));
            }
            default: {
                return IntStream.range(0, 2 * (int) Math.pow(Math.pow(2, level) / 4, 2))
                        .mapToObj(index -> (byte) 0)
                        .collect(Collectors.toList());
            }
        }
    }

    public List<Byte> getTiles() {
        return tiles;
    }

    public int getLevel() {
        return level;
    }

    public void reset() {
        tiles = createTiles(level);
    }

    public void putTile(int row, int col) {
        if (level >= 2) {
            int stride = (int) Math.pow(2, level) / 4;
            int cellRow = row / 4;
            int cellCol = col / 4;
            int cellOffset = 2 * (cellRow * stride + cellCol);
            int tileRow = row % 4;
            int tileCol = col % 4;
            int tileOffset = 4 * tileRow + tileCol;
            int offset = tileOffset / 8;
            int index = tileOffset % 8;
            byte data = tiles.get(cellOffset + offset);
            tiles.set(cellOffset + offset, (byte) (data | (1 << index)));
        } else {
            int tileOffset = 4 * row + col;
            int index = tileOffset % 8;
            byte data = tiles.get(0);
            tiles.set(0, (byte) (data | (1 << index)));
        }
    }

    public boolean hasTile(int row, int col) {
        if (level >= 2) {
            int stride = (int) Math.pow(2, level) / 4;
            int cellRow = row / 4;
            int cellCol = col / 4;
            int cellOffset = 2 * (cellRow * stride + cellCol);
            int tileRow = row % 4;
            int tileCol = col % 4;
            int tileOffset = 4 * tileRow + tileCol;
            int offset = tileOffset / 8;
            int index = tileOffset % 8;
            byte data = tiles.get(cellOffset + offset);
            return (byte) (data & (1 << index)) != 0;
        } else {
            int tileOffset = 4 * row + col;
            int index = tileOffset % 8;
            byte data = tiles.get(0);
            return (byte) (data & (1 << index)) == 0x01;
        }
    }

    public String dump() {
        StringBuilder builder = new StringBuilder();

        int size = (int) Math.pow(2, level);

        IntStream.range(0, size).forEach(row -> IntStream.range(0, size).forEach(col -> {
            if (hasTile(row, col)) {
                builder.append("X");
            } else {
                builder.append("O");
            }
            if (col % size == size - 1) {
                builder.append("\n");
            }
        }));

        return builder.toString();
    }

    @Override
    public String toString() {
        return "Level{" +
                "level=" + level +
                ", completed=" + countTiles() +
                '}';
    }

    public Tiles toTiles() {
        return Tiles.builder()
                .withLevel(level)
                .withCompleted(countTiles())
                .build();
    }

    private int countTiles() {
        if (level == 0) {
            return (tiles.get(0) & 0x1) != 0 ? 1 : 0;
        }

        if (level == 1) {
            return COUNT_TABLE.get(tiles.get(0));
        }

        return tiles.stream().mapToInt(tile -> COUNT_TABLE.get(0xFF & ((int) tile))).sum();
    }
}
