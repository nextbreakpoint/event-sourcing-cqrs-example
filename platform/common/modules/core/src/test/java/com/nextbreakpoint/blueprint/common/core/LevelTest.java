package com.nextbreakpoint.blueprint.common.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class LevelTest {
    @Test
    void shouldCreateLevel() {
        final Level level0 = Level.createEmpty(0);
        assertThat(level0.getLevel()).isEqualTo(0);
        assertThat(level0.getTiles()).isEqualTo(List.of((byte) 0));

        final Level level1 = Level.createEmpty(1);
        assertThat(level1.getLevel()).isEqualTo(1);
        assertThat(level1.getTiles()).isEqualTo(List.of((byte) 0));

        final Level level2 = Level.createEmpty(2);
        assertThat(level2.getLevel()).isEqualTo(2);
        assertThat(level2.getTiles()).isEqualTo(IntStream.range(0, 2).mapToObj(index -> (byte) 0).collect(Collectors.toList()));

        final Level level3 = Level.createEmpty(3);
        assertThat(level3.getLevel()).isEqualTo(3);
        assertThat(level3.getTiles()).isEqualTo(IntStream.range(0, 8).mapToObj(index -> (byte) 0).collect(Collectors.toList()));

        final Level level4 = Level.createEmpty(4);
        assertThat(level4.getLevel()).isEqualTo(4);
        assertThat(level4.getTiles()).isEqualTo(IntStream.range(0, 32).mapToObj(index -> (byte) 0).collect(Collectors.toList()));

        final Level level5 = Level.createEmpty(5);
        assertThat(level5.getLevel()).isEqualTo(5);
        assertThat(level5.getTiles()).isEqualTo(IntStream.range(0, 128).mapToObj(index -> (byte) 0).collect(Collectors.toList()));

        final Level level6 = Level.createEmpty(6);
        assertThat(level6.getLevel()).isEqualTo(6);
        assertThat(level6.getTiles()).isEqualTo(IntStream.range(0, 512).mapToObj(index -> (byte) 0).collect(Collectors.toList()));

        final Level level7 = Level.createEmpty(7);
        assertThat(level7.getLevel()).isEqualTo(7);
        assertThat(level7.getTiles()).isEqualTo(IntStream.range(0, 2048).mapToObj(index -> (byte) 0).collect(Collectors.toList()));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForPutTile")
    void shouldPutTileForLevel(Level level) {
        int size = (int) Math.pow(2, level.getLevel() - 1);
        IntStream.range(0, size).forEach(row -> IntStream.range(0, size).forEach(col -> {
            level.putTile(row, col);
            assertThat(level.hasTile(row, col)).isEqualTo(true);
            assertThat(level.toTiles().getCompleted()).isEqualTo(1);
            level.reset();
        }));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForPutTile")
    void shouldPutAllTilesForLevel(Level level) {
        int size = (int) Math.pow(2, level.getLevel() - 1);
        IntStream.range(0, size).forEach(row -> IntStream.range(0, size).forEach(col -> {
            level.putTile(row, col);
        }));
        assertThat(level.toTiles().getCompleted()).isEqualTo(size * size);
    }

    @Test
    void shouldPutTileForLevel0() {
        Level level = Level.createEmpty(0);
        level.putTile(0, 0);
        assertThat(level.hasTile(0, 0)).isEqualTo(true);
        assertThat(level.toTiles().getCompleted()).isEqualTo(1);
        level.reset();
    }

    @Test
    void shouldPutTile() {
        Level level2 = Level.createEmpty(2);

        level2.putTile(0, 0);
        level2.putTile(3, 0);
        level2.putTile(3, 2);
        level2.putTile(2, 1);
        level2.putTile(2, 3);
        level2.putTile(1, 1);
        System.out.println(level2.dump());

        assertThat(level2.toTiles().getLevel()).isEqualTo(2);
        assertThat(level2.toTiles().getCompleted()).isEqualTo(6);

        assertThat(level2.getTiles()).isEqualTo(List.of((byte) 33, (byte) 90));

        assertThat(level2.dump()).isEqualTo(
                "XOOO\n" +
                "OXOO\n" +
                "OXOX\n" +
                "XOXO\n"
        );

        Level level3 = Level.createEmpty(3);

        level3.putTile(0, 0);
        level3.putTile(7, 5);
        level3.putTile(3, 2);
        level3.putTile(2, 1);
        level3.putTile(4, 3);
        level3.putTile(1, 6);
        level3.putTile(7, 7);
        System.out.println(level3.dump());

        assertThat(level3.toTiles().getLevel()).isEqualTo(3);
        assertThat(level3.toTiles().getCompleted()).isEqualTo(7);

        assertThat(level3.getTiles()).isEqualTo(List.of((byte) 1, (byte) 66, (byte) 64, (byte) 0, (byte) 8, (byte) 0, (byte) 0, (byte) -96));

        assertThat(level3.dump()).isEqualTo(
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
                Arguments.of(Level.createEmpty(1)),
                Arguments.of(Level.createEmpty(2)),
                Arguments.of(Level.createEmpty(3)),
                Arguments.of(Level.createEmpty(4)),
                Arguments.of(Level.createEmpty(5)),
                Arguments.of(Level.createEmpty(6)),
                Arguments.of(Level.createEmpty(7))
        );
    }
}
