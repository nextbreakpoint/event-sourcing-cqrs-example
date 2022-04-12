package com.nextbreakpoint.blueprint.common.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TilesBitmapTest {
    @Test
    void shouldCreateEmptyBitmap() {
        final TilesBitmap bitmap = TilesBitmap.empty();

        assertThat(bitmap.getBitmap()).isEqualTo(ByteBuffer.allocate(2732));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForPutTile")
    void shouldPutTileForLevel(int level) {
        final TilesBitmap bitmap = TilesBitmap.empty();

        int size = level > 0 ? (int) Math.pow(2, level) : 1;

        IntStream.range(0, size).forEach(row -> IntStream.range(0, size).forEach(col -> {
            bitmap.putTile(level, row, col);
            assertThat(bitmap.hasTile(level, row, col)).isEqualTo(true);
            assertThat(bitmap.toTiles(level).getCompleted()).isEqualTo(1);
            bitmap.reset();
        }));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForPutTile")
    void shouldPutAllTilesForLevel(int level) {
        final TilesBitmap bitmap = TilesBitmap.empty();

        int size = level > 0 ? (int) Math.pow(2, level) : 1;

        IntStream.range(0, size).forEach(row -> IntStream.range(0, size).forEach(col -> {
            bitmap.putTile(level, row, col);
            assertThat(bitmap.toTiles(level).getCompleted()).isEqualTo(size * row + col + 1);
        }));
    }

    @Test
    void shouldPutTile() {
        TilesBitmap bitmap = TilesBitmap.empty();

        bitmap.putTile(2, 0, 0);
        bitmap.putTile(2, 3, 0);
        bitmap.putTile(2, 3, 2);
        bitmap.putTile(2, 2, 1);
        bitmap.putTile(2, 2, 3);
        bitmap.putTile(2, 1, 1);
        System.out.println(bitmap.dump(2));

        assertThat(bitmap.toTiles(2).getLevel()).isEqualTo(2);
        assertThat(bitmap.toTiles(2).getCompleted()).isEqualTo(6);

        assertThat(bitmap.dump(2)).isEqualTo(
                "XOOO\n" +
                "OXOO\n" +
                "OXOX\n" +
                "XOXO\n"
        );

        bitmap.putTile(3, 0, 0);
        bitmap.putTile(3, 7, 5);
        bitmap.putTile(3, 3, 2);
        bitmap.putTile(3, 2, 1);
        bitmap.putTile(3, 4, 3);
        bitmap.putTile(3, 1, 6);
        bitmap.putTile(3, 7, 7);
        System.out.println(bitmap.dump(3));

        assertThat(bitmap.toTiles(3).getLevel()).isEqualTo(3);
        assertThat(bitmap.toTiles(3).getCompleted()).isEqualTo(7);

        assertThat(bitmap.dump(3)).isEqualTo(
                "XOOOOOOO\n" +
                "OOOOOOXO\n" +
                "OXOOOOOO\n" +
                "OOXOOOOO\n" +
                "OOOXOOOO\n" +
                "OOOOOOOO\n" +
                "OOOOOOOO\n" +
                "OOOOOXOX\n"
        );
    }

    private static Stream<Arguments> provideArgumentsForPutTile() {
        return Stream.of(
                Arguments.of(0),
                Arguments.of(1),
                Arguments.of(2),
                Arguments.of(3),
                Arguments.of(4),
                Arguments.of(5),
                Arguments.of(6),
                Arguments.of(7)
        );
    }
}
