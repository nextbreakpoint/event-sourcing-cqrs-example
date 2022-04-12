package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class RenderTest {
    @ParameterizedTest
    @MethodSource("generateValuesForNextTile")
    void shouldReturnNextTile(Tile tile, Tile expectedTile) {
        List<Tile> result = Render.generateTiles(tile, 8, TilesBitmap.empty());
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedTile);
    }

    @ParameterizedTest
    @MethodSource("generateValuesForAllTiles")
    void shouldGenerateAllTiles(Tile tile) {
        List<Tile> tiles = new ArrayList<>();
        List<Tile> result = List.of(tile);
        for (;;) {
            result = Render.generateTiles(result.get(0), 8, TilesBitmap.empty());
            if (result.isEmpty()) {
                break;
            }
            assertThat(result).hasSize(1);
            System.out.println(result.get(0));
            tiles.add(result.get(0));
        }
        assertThat(tiles).hasSize(1364);
    }

    private static Stream<Arguments> generateValuesForNextTile() {
        return Stream.of(
                Arguments.of(makeTile(2, 0, 0), makeTile(3, 0, 0)),
                Arguments.of(makeTile(3, 0, 0), makeTile(3, 0, 1)),
                Arguments.of(makeTile(3, 0, 1), makeTile(3, 1, 0)),
                Arguments.of(makeTile(3, 1, 0), makeTile(3, 1, 1)),
                Arguments.of(makeTile(3, 1, 1), makeTile(4, 0, 0)),
                Arguments.of(makeTile(4, 0, 0), makeTile(4, 0, 1)),
                Arguments.of(makeTile(4, 0, 1), makeTile(4, 0, 2)),
                Arguments.of(makeTile(4, 0, 2), makeTile(4, 0, 3)),
                Arguments.of(makeTile(4, 0, 3), makeTile(4, 1, 0)),
                Arguments.of(makeTile(4, 1, 0), makeTile(4, 1, 1)),
                Arguments.of(makeTile(4, 1, 1), makeTile(4, 1, 2)),
                Arguments.of(makeTile(4, 1, 2), makeTile(4, 1, 3)),
                Arguments.of(makeTile(4, 1, 3), makeTile(4, 2, 0)),
                Arguments.of(makeTile(4, 2, 0), makeTile(4, 2, 1)),
                Arguments.of(makeTile(4, 2, 1), makeTile(4, 2, 2)),
                Arguments.of(makeTile(4, 2, 2), makeTile(4, 2, 3)),
                Arguments.of(makeTile(4, 2, 3), makeTile(4, 3, 0)),
                Arguments.of(makeTile(4, 3, 0), makeTile(4, 3, 1)),
                Arguments.of(makeTile(4, 3, 1), makeTile(4, 3, 2)),
                Arguments.of(makeTile(4, 3, 2), makeTile(4, 3, 3)),
                Arguments.of(makeTile(4, 3, 3), makeTile(5, 0, 0)),

                Arguments.of(makeTile(2, 1, 1), makeTile(3, 2, 2)),
                Arguments.of(makeTile(3, 2, 2), makeTile(3, 2, 3)),
                Arguments.of(makeTile(3, 2, 3), makeTile(3, 3, 2)),
                Arguments.of(makeTile(3, 3, 2), makeTile(3, 3, 3)),
                Arguments.of(makeTile(3, 3, 3), makeTile(4, 4, 4)),
                Arguments.of(makeTile(4, 4, 4), makeTile(4, 4, 5)),
                Arguments.of(makeTile(4, 4, 5), makeTile(4, 4, 6)),
                Arguments.of(makeTile(4, 4, 6), makeTile(4, 4, 7)),
                Arguments.of(makeTile(4, 4, 7), makeTile(4, 5, 4)),
                Arguments.of(makeTile(4, 5, 4), makeTile(4, 5, 5)),
                Arguments.of(makeTile(4, 5, 5), makeTile(4, 5, 6)),
                Arguments.of(makeTile(4, 5, 6), makeTile(4, 5, 7)),
                Arguments.of(makeTile(4, 5, 7), makeTile(4, 6, 4)),
                Arguments.of(makeTile(4, 6, 4), makeTile(4, 6, 5)),
                Arguments.of(makeTile(4, 6, 5), makeTile(4, 6, 6)),
                Arguments.of(makeTile(4, 6, 6), makeTile(4, 6, 7)),
                Arguments.of(makeTile(4, 6, 7), makeTile(4, 7, 4)),
                Arguments.of(makeTile(4, 7, 4), makeTile(4, 7, 5)),
                Arguments.of(makeTile(4, 7, 5), makeTile(4, 7, 6)),
                Arguments.of(makeTile(4, 7, 6), makeTile(4, 7, 7)),
                Arguments.of(makeTile(4, 7, 7), makeTile(5, 8, 8)),

                Arguments.of(makeTile(5, 15, 15), makeTile(6, 16, 16)),

                Arguments.of(makeTile(6, 31, 31), makeTile(7, 32, 32)),

                Arguments.of(makeTile(7, 127, 126), makeTile(7, 127, 127))
        );
    }

    private static Stream<Arguments> generateValuesForAllTiles() {
        return Stream.of(
                Arguments.of(makeTile(2, 0, 0)),
                Arguments.of(makeTile(2, 0, 1)),
                Arguments.of(makeTile(2, 0, 2)),
                Arguments.of(makeTile(2, 0, 3)),
                Arguments.of(makeTile(2, 1, 0)),
                Arguments.of(makeTile(2, 1, 1)),
                Arguments.of(makeTile(2, 1, 2)),
                Arguments.of(makeTile(2, 1, 3)),
                Arguments.of(makeTile(2, 2, 0)),
                Arguments.of(makeTile(2, 2, 1)),
                Arguments.of(makeTile(2, 2, 2)),
                Arguments.of(makeTile(2, 2, 3)),
                Arguments.of(makeTile(2, 3, 0)),
                Arguments.of(makeTile(2, 3, 1)),
                Arguments.of(makeTile(2, 3, 2)),
                Arguments.of(makeTile(2, 3, 3))
        );
    }

    private static Tile makeTile(int level, int row, int col) {
        return Tile.builder().withLevel(level).withRow(row).withCol(col).build();
    }
}
