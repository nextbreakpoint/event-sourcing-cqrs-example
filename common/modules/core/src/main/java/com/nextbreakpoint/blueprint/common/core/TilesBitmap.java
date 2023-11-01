package com.nextbreakpoint.blueprint.common.core;

import lombok.Data;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class TilesBitmap {
    private byte[] bitmap;

    private static List<Integer> COUNT_TABLE = IntStream.range(0, 256)
            .mapToObj(TilesBitmap::countBits).toList();

    private static List<Integer> OFFSET_TABLE = IntStream.range(0, 8)
            .mapToObj(TilesBitmap::levelOffset).toList();

    private static int countBits(int value) {
        return IntStream.range(0, 8)
                .map(shift -> value & (1 << shift))
                .map(mask -> mask != 0 ? 1 : 0)
                .sum();
    }

    private static int levelOffset(int level) {
        if (level == 0) {
            return 0;
        }

        if (level == 1) {
            return 1;
        }

        return IntStream.range(2, level)
                .map(l -> 2 * (int) Math.pow(Math.pow(2, l) / 4, 2))
                .sum() + 2;
    }

    private TilesBitmap(ByteBuffer bitmap) {
        this.bitmap = bitmap.array();
    }

    public static TilesBitmap of(ByteBuffer bitmap) {
        return new TilesBitmap(bitmap);
    }

    public static TilesBitmap empty() {
        int size = IntStream.range(2, 8)
                .map(level -> 2 * (int) Math.pow(Math.pow(2, level) / 4, 2))
                .sum() + 2;

        return new TilesBitmap(ByteBuffer.wrap(new byte[size]));
    }

    public ByteBuffer getBitmap() {
        return ByteBuffer.wrap(bitmap);
    }

    public TilesBitmap reset() {
        IntStream.range(0, bitmap.length)
                .forEach(index -> bitmap[index] = (byte) 0);
        return this;
    }

    public TilesBitmap putTile(int level, int row, int col) {
        int levelOffset = OFFSET_TABLE.get(level);
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
            byte data = bitmap[levelOffset + cellOffset + offset];
            bitmap[levelOffset + cellOffset + offset] = (byte) (data | (1 << index));
        } else {
            int tileOffset = 4 * row + col;
            int index = tileOffset % 8;
            byte data = bitmap[levelOffset];
            bitmap[levelOffset] = (byte) (data | (1 << index));
        }
        return this;
    }

    public boolean hasTile(int level, int row, int col) {
        int levelOffset = OFFSET_TABLE.get(level);
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
            byte data = bitmap[levelOffset + cellOffset + offset];
            return (byte) (data & (1 << index)) != 0;
        } else {
            int tileOffset = 4 * row + col;
            int index = tileOffset % 8;
            byte data = bitmap[levelOffset];
            return (byte) (data & (1 << index)) != 0;
        }
    }

    public String dump(int level) {
        StringBuilder builder = new StringBuilder();

        int size = (int) Math.pow(2, level);

        IntStream.range(0, size).forEach(row -> IntStream.range(0, size).forEach(col -> {
            if (hasTile(level, row, col)) {
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

    public Tiles toTiles(int level) {
        final int total = (int) Math.rint(Math.pow(2, level * 2));

        return Tiles.builder()
                .withLevel(level)
                .withTotal(total)
                .withCompleted(countTiles(level))
                .build();
    }

    private int countTiles(int level) {
        int levelOffset = OFFSET_TABLE.get(level);

        if (level < 2) {
            return COUNT_TABLE.get(bitmap[levelOffset]);
        }

        int size = 2 * (int) Math.pow(Math.pow(2, level) / 4, 2);

        return IntStream.range(0, size)
                .map(index -> COUNT_TABLE.get(0xFF & ((int) bitmap[levelOffset + index]))).sum();
    }
}
