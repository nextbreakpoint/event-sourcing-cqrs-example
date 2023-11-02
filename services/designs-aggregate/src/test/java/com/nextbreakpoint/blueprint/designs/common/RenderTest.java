package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.designs.TestConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.*;
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

    @ParameterizedTest
    @MethodSource("generateValuesForGetTopicName")
    void shouldGetTopicName(String prefix, int level, String expectedTopicName) {
        assertThat(Render.getTopicName(prefix, level)).isEqualTo(expectedTopicName);
    }

    @ParameterizedTest
    @MethodSource("generateTileRenderRequestedEvents")
    void shouldCreateRenderKey(TileRenderRequested event, String expectedKey) {
        assertThat(Render.createRenderKey(event)).isEqualTo(expectedKey);
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

    private static Stream<Arguments> generateValuesForGetTopicName() {
        return Stream.of(
                Arguments.of("topix-a", 1, "topix-a-0"),
                Arguments.of("topix-b", 2, "topix-b-0"),
                Arguments.of("topix-c", 3, "topix-c-1"),
                Arguments.of("topix-d", 4, "topix-d-1"),
                Arguments.of("topix-e", 5, "topix-e-1"),
                Arguments.of("topix-f", 6, "topix-f-2"),
                Arguments.of("topix-g", 7, "topix-g-3")
        );
    }

    private static Stream<Arguments> generateTileRenderRequestedEvents() {
        return Stream.of(
                Arguments.of(
                        TileRenderRequested.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withData(DATA_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withLevel(0)
                                .withRow(0)
                                .withCol(0)
                                .build(),
                        "%s/%s/%d/%04d%04d.png".formatted(DESIGN_ID_1, COMMAND_ID_1, 0, 0, 0)
                ),
                Arguments.of(
                        TileRenderRequested.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withData(DATA_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withLevel(3)
                                .withRow(1)
                                .withCol(2)
                                .build(),
                        "%s/%s/%d/%04d%04d.png".formatted(DESIGN_ID_2, COMMAND_ID_2, 3, 1, 2)
                )
        );
    }

    private static Tile makeTile(int level, int row, int col) {
        return Tile.builder().withLevel(level).withRow(row).withCol(col).build();
    }
}
