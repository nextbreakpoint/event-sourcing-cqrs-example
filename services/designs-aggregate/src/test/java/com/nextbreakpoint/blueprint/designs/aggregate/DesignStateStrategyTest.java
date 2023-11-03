package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILES_RENDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesignStateStrategyTest {
    private static final UUID DESIGN_ID_1 = new UUID(0L, 1L);
    private static final UUID DESIGN_ID_2 = new UUID(0L, 2L);

    private static final UUID USER_ID_1 = new UUID(0L, 1L);
    private static final UUID USER_ID_2 = new UUID(0L, 2L);

    private static final String REVISION_0 = "0000000000000000-0000000000000000";
    private static final String REVISION_1 = "0000000000000000-0000000000000001";
    private static final String REVISION_2 = "0000000000000000-0000000000000002";

    private static final UUID COMMAND_ID_1 = new UUID(1L, 1L);
    private static final UUID COMMAND_ID_2 = new UUID(1L, 2l);
    private static final UUID COMMAND_ID_3 = new UUID(1L, 3l);
    private static final UUID COMMAND_ID_4 = new UUID(1L, 4l);
    private static final UUID COMMAND_ID_5 = new UUID(1L, 5l);
    private static final UUID COMMAND_ID_6 = new UUID(1L, 6l);
    private static final UUID COMMAND_ID_7 = new UUID(1L, 7l);
    private static final UUID COMMAND_ID_8 = new UUID(1L, 8l);

    private static final String DATA_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String DATA_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String DATA_3 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 50] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private static final TilesBitmap bitmap0 = TilesBitmap.empty();
    private static final TilesBitmap bitmap1 = TilesBitmap.empty().putTile(0, 0, 0);
    private static final TilesBitmap bitmap2 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 0, 0);
    private static final TilesBitmap bitmap3 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 0, 0).putTile(1, 1, 0);
    private static final TilesBitmap bitmap4 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 0, 0).putTile(1, 1, 0).putTile(2, 2, 1);

    private static final DesignInsertRequested designInsertRequested = DesignInsertRequested.builder()
            .withUserId(USER_ID_1)
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_1)
            .withData(DATA_1)
            .build();

    private static final DesignInsertRequested invalidDesignInsertRequested = DesignInsertRequested.builder()
            .withUserId(USER_ID_2)
            .withDesignId(DESIGN_ID_2)
            .withCommandId(COMMAND_ID_1)
            .withData(DATA_2)
            .build();

    private static final DesignInsertRequested alternativeDesignInsertRequested = DesignInsertRequested.builder()
            .withUserId(USER_ID_2)
            .withDesignId(DESIGN_ID_2)
            .withCommandId(COMMAND_ID_1)
            .withData(DATA_3)
            .build();

    private static final DesignUpdateRequested designUpdateRequested = DesignUpdateRequested.builder()
            .withUserId(USER_ID_1)
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_2)
            .withData(DATA_2)
            .withPublished(false)
            .build();

    private static final DesignUpdateRequested anotherDesignUpdateRequested = DesignUpdateRequested.builder()
            .withUserId(USER_ID_1)
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_3)
            .withData(DATA_2)
            .withPublished(true)
            .build();

    private static final DesignUpdateRequested invalidDesignUpdateRequested = DesignUpdateRequested.builder()
            .withUserId(USER_ID_2)
            .withDesignId(DESIGN_ID_2)
            .withCommandId(COMMAND_ID_3)
            .withData(DATA_2)
            .withPublished(true)
            .build();

    private static final DesignDeleteRequested designDeleteRequested = DesignDeleteRequested.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_8)
            .withUserId(USER_ID_1)
            .build();

    private static final DesignDeleteRequested invalidDesignDeleteRequested = DesignDeleteRequested.builder()
            .withDesignId(DESIGN_ID_2)
            .withCommandId(COMMAND_ID_8)
            .withUserId(USER_ID_2)
            .build();

    private static final TilesRendered tilesRendered1 = TilesRendered.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_4)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withData(DATA_2)
            .withTiles(List.of(
                    Tile.builder().withLevel(0).withRow(0).withCol(0).build()
            ))
            .build();

    private static final TilesRendered tilesRendered2 = TilesRendered.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_5)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withData(DATA_2)
            .withTiles(List.of(
                    Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(0).withCol(0).build()
            ))
            .build();

    private static final TilesRendered tilesRendered3 = TilesRendered.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_6)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withData(DATA_2)
            .withTiles(List.of(
                    Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(1).withCol(0).build()
            ))
            .build();

    private static final TilesRendered tilesRendered4 = TilesRendered.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_7)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withData(DATA_2)
            .withTiles(List.of(
                    Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(1).withCol(0).build(),
                    Tile.builder().withLevel(2).withRow(2).withCol(1).build()
            ))
            .build();

    private static final TilesRendered invalidTilesRendered = TilesRendered.builder()
            .withDesignId(DESIGN_ID_2)
            .withCommandId(COMMAND_ID_7)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withData(DATA_2)
            .withTiles(List.of(
                    Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(0).withCol(0).build(),
                    Tile.builder().withLevel(1).withRow(1).withCol(0).build(),
                    Tile.builder().withLevel(2).withRow(2).withCol(1).build()
            ))
            .build();

    private static final InputMessage designInsertRequestedMessage = TestUtils.createInputMessage(
            designInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, UUID.randomUUID(), designInsertRequested, REVISION_0, dateTime.minusHours(9)
    );

    private static final InputMessage duplicateDesignInsertRequestedMessage = TestUtils.createInputMessage(
            designInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, UUID.randomUUID(), designInsertRequested, REVISION_0, dateTime.minusHours(8)
    );

    private static final InputMessage invalidDesignInsertRequestedMessage = TestUtils.createInputMessage(
            invalidDesignInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, UUID.randomUUID(), invalidDesignInsertRequested, REVISION_0, dateTime.minusHours(8)
    );

    private static final InputMessage alternativeDesignInsertRequestedMessage = TestUtils.createInputMessage(
            alternativeDesignInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, UUID.randomUUID(), alternativeDesignInsertRequested, REVISION_0, dateTime.minusHours(8)
    );

    private static final InputMessage designUpdateRequestedMessage = TestUtils.createInputMessage(
            designUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, UUID.randomUUID(), designUpdateRequested, REVISION_1, dateTime.minusHours(8)
    );

    private static final InputMessage anotherDesignUpdateRequestedMessage = TestUtils.createInputMessage(
            anotherDesignUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, UUID.randomUUID(), anotherDesignUpdateRequested, REVISION_2, dateTime.minusHours(7)
    );

    private static final InputMessage invalidDesignUpdateRequestedMessage = TestUtils.createInputMessage(
            invalidDesignUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, UUID.randomUUID(), invalidDesignUpdateRequested, REVISION_2, dateTime.minusHours(7)
    );

    private static final InputMessage designDeleteRequestedMessage = TestUtils.createInputMessage(
            designDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, UUID.randomUUID(), designDeleteRequested, REVISION_2, dateTime.minusHours(1)
    );

    private static final InputMessage duplicateDesignDeleteRequestedMessage = TestUtils.createInputMessage(
            designDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, UUID.randomUUID(), designDeleteRequested, REVISION_2, dateTime.minusHours(0)
    );

    private static final InputMessage invalidDesignDeleteRequestedMessage = TestUtils.createInputMessage(
            invalidDesignDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, UUID.randomUUID(), invalidDesignDeleteRequested, REVISION_2, dateTime.minusHours(1)
    );

    private static final InputMessage tileRenderedMessage1 = TestUtils.createInputMessage(
            tilesRendered1.getDesignId().toString(), TILES_RENDERED, UUID.randomUUID(), tilesRendered1, REVISION_2, dateTime.minusHours(6)
    );

    private static final InputMessage tileRenderedMessage2 = TestUtils.createInputMessage(
            tilesRendered2.getDesignId().toString(), TILES_RENDERED, UUID.randomUUID(), tilesRendered2, REVISION_2, dateTime.minusHours(5)
    );

    private static final InputMessage tileRenderedMessage3 = TestUtils.createInputMessage(
            tilesRendered3.getDesignId().toString(), TILES_RENDERED, UUID.randomUUID(), tilesRendered3, REVISION_2, dateTime.minusHours(4)
    );

    private static final InputMessage tileRenderedMessage4 = TestUtils.createInputMessage(
            tilesRendered4.getDesignId().toString(), TILES_RENDERED, UUID.randomUUID(), tilesRendered4, REVISION_2, dateTime.minusHours(3)
    );

    private static final InputMessage invalidTileRenderedMessage = TestUtils.createInputMessage(
            invalidTilesRendered.getDesignId().toString(), TILES_RENDERED, UUID.randomUUID(), invalidTilesRendered, REVISION_2, dateTime.minusHours(3)
    );

    private static final InputMessage unknownMessage = TestUtils.createInputMessage(
            designInsertRequested.getDesignId().toString(), "UNKNOWN", UUID.randomUUID(), "", REVISION_2, dateTime.minusHours(0)
    );

    private static final Design state1 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_1)
            .withUserId(USER_ID_1)
            .withData(DATA_1)
            .withChecksum(Checksum.of(DATA_1))
            .withRevision(REVISION_0)
            .withStatus("CREATED")
            .withLevels(3)
            .withBitmap(bitmap0.getBitmap())
            .withPublished(false)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(9))
            .build();

    private static final Design state2 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_2)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withStatus("UPDATED")
            .withLevels(3)
            .withBitmap(bitmap0.getBitmap())
            .withPublished(false)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(8))
            .build();

    private static final Design state3 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_3)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("UPDATED")
            .withLevels(8)
            .withBitmap(bitmap0.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(7))
            .build();

    private static final Design state4 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_3)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("UPDATED")
            .withLevels(8)
            .withBitmap(bitmap1.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(6))
            .build();

    private static final Design state5 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_3)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("UPDATED")
            .withLevels(8)
            .withBitmap(bitmap2.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(5))
            .build();

    private static final Design state6 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_3)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("UPDATED")
            .withLevels(8)
            .withBitmap(bitmap3.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(4))
            .build();

    private static final Design state7 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_3)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("UPDATED")
            .withLevels(8)
            .withBitmap(bitmap4.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(3))
            .build();

    private static final Design state8 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_8)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("DELETED")
            .withLevels(8)
            .withBitmap(bitmap4.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(1))
            .build();

    private static final Design state9 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_8)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("DELETED")
            .withLevels(8)
            .withBitmap(bitmap4.getBitmap())
            .withPublished(true)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(1))
            .build();

    private static final Design state10 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_8)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("DELETED")
            .withLevels(3)
            .withBitmap(bitmap1.getBitmap())
            .withPublished(false)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(1))
            .build();

    private static final Design state11 = Design.builder()
            .withDesignId(DESIGN_ID_1)
            .withCommandId(COMMAND_ID_8)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withStatus("DELETED")
            .withLevels(3)
            .withBitmap(bitmap1.getBitmap())
            .withPublished(false)
            .withCreated(dateTime.minusHours(9))
            .withUpdated(dateTime.minusHours(1))
            .build();

    private static final Design state12 = Design.builder()
            .withDesignId(DESIGN_ID_2)
            .withCommandId(COMMAND_ID_1)
            .withUserId(USER_ID_2)
            .withData(DATA_3)
            .withChecksum(Checksum.of(DATA_3))
            .withRevision(REVISION_0)
            .withStatus("CREATED")
            .withLevels(3)
            .withBitmap(bitmap0.getBitmap())
            .withPublished(false)
            .withCreated(dateTime.minusHours(8))
            .withUpdated(dateTime.minusHours(8))
            .build();

    private final DesignMergeStrategy strategy = new DesignMergeStrategy();

    @Test
    void shouldNotAlterStateWhenMessagesIsEmpty() {
        final LocalDateTime createTime = dateTime.minusDays(1).truncatedTo(ChronoUnit.MILLIS);
        final LocalDateTime updateTime = dateTime.truncatedTo(ChronoUnit.MILLIS);

        final Design initialState = Design.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withUserId(USER_ID_1)
                .withData(DATA_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(REVISION_0)
                .withStatus("CREATED")
                .withLevels(3)
                .withBitmap(bitmap0.getBitmap())
                .withPublished(false)
                .withCreated(createTime)
                .withUpdated(updateTime)
                .build();

        final Design expectedState = Design.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withUserId(USER_ID_1)
                .withData(DATA_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(REVISION_0)
                .withStatus("CREATED")
                .withLevels(3)
                .withBitmap(bitmap0.getBitmap())
                .withPublished(false)
                .withCreated(createTime)
                .withUpdated(updateTime)
                .build();

        final Optional<Design> actualState = strategy.applyEvents(initialState, List.of());
        assertThat(actualState).isPresent().hasValue(expectedState);
    }

    @ParameterizedTest
    @MethodSource("someValidMessages")
    void shouldUpdateState(List<InputMessage> messages, Design expectedState) {
        final Optional<Design> actualState = strategy.applyEvents(null, messages);
        assertThat(actualState).isPresent().hasValue(expectedState);
    }

    @ParameterizedTest
    @MethodSource("someInvalidMessages")
    void shouldThrowIllegalStateException(List<InputMessage> messages) {
        assertThatThrownBy(() -> strategy.applyEvents(null, messages))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Stream<Arguments> someValidMessages() {
        return Stream.of(
                Arguments.arguments(List.of(
                        designInsertRequestedMessage
                ), state1),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage
                ), state2),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage
                ), state3),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage,
                        tileRenderedMessage1
                ), state4),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage,
                        tileRenderedMessage1,
                        tileRenderedMessage2
                ), state5),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage,
                        tileRenderedMessage1,
                        tileRenderedMessage2,
                        tileRenderedMessage3
                ), state6),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage,
                        tileRenderedMessage1,
                        tileRenderedMessage2,
                        tileRenderedMessage3,
                        tileRenderedMessage4
                ), state7),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage,
                        tileRenderedMessage1,
                        tileRenderedMessage2,
                        tileRenderedMessage3,
                        tileRenderedMessage4,
                        designDeleteRequestedMessage
                ), state8),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        anotherDesignUpdateRequestedMessage,
                        tileRenderedMessage1,
                        tileRenderedMessage2,
                        tileRenderedMessage3,
                        tileRenderedMessage4,
                        designDeleteRequestedMessage,
                        unknownMessage
                ), state9),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        tileRenderedMessage1,
                        designDeleteRequestedMessage,
                        tileRenderedMessage2,
                        tileRenderedMessage3,
                        tileRenderedMessage4,
                        anotherDesignUpdateRequestedMessage
                ), state10),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        tileRenderedMessage1,
                        designDeleteRequestedMessage,
                        duplicateDesignDeleteRequestedMessage
                ), state11),
                Arguments.arguments(List.of(
                        alternativeDesignInsertRequestedMessage
                ), state12)
        );
    }

    private static Stream<Arguments> someInvalidMessages() {
        return Stream.of(
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        duplicateDesignInsertRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designUpdateRequestedMessage,
                        duplicateDesignInsertRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        designDeleteRequestedMessage,
                        duplicateDesignInsertRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        tileRenderedMessage1,
                        duplicateDesignInsertRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designUpdateRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designDeleteRequestedMessage
                )),
                Arguments.arguments(List.of(
                        tileRenderedMessage1
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        invalidDesignInsertRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        invalidDesignUpdateRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        invalidDesignDeleteRequestedMessage
                )),
                Arguments.arguments(List.of(
                        designInsertRequestedMessage,
                        invalidTileRenderedMessage
                ))
        );
    }
}