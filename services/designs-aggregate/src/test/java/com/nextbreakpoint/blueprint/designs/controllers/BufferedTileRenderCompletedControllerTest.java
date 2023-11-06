package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TilesRenderedOutputMapper;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILES_RENDERED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BufferedTileRenderCompletedControllerTest {
    private static final Mapper<InputMessage, TileRenderCompleted> inputMapper = new TileRenderCompletedInputMapper();
    private static final MessageMapper<TilesRendered, OutputMessage> outputMapper = new TilesRenderedOutputMapper(MESSAGE_SOURCE);

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter emitter = mock();
    private final DesignEventStore eventStore = mock();

    private final BufferedTileRenderCompletedController controller = new BufferedTileRenderCompletedController(eventStore, inputMapper, outputMapper, emitter);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldPublishAMessageToInformThatTilesHaveBeingRendered(Design design, List<InputMessage> inputMessages, OutputMessage expectedOutputMessage) {
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessages).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(emitter).send(assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        }));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldDoNothingWhenDesignDoesNotExist() {
        when(eventStore.findDesign(DESIGN_ID_1)).thenReturn(Single.just(Optional.empty()));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(someInputMessages()).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).findDesign(DESIGN_ID_1);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenFindDesignFails() {
        final RuntimeException exception = new RuntimeException();
        when(eventStore.findDesign(DESIGN_ID_1)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(someInputMessages()).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).findDesign(DESIGN_ID_1);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenInputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final Mapper<InputMessage, TileRenderCompleted> mockedInputMapper = mock();
        when(mockedInputMapper.transform(any(InputMessage.class))).thenThrow(exception);

        final var controller = new BufferedTileRenderCompletedController(eventStore, mockedInputMapper, outputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(someInputMessages()).toCompletable().await()).isEqualTo(exception);

        verify(mockedInputMapper).transform(any(InputMessage.class));
        verifyNoMoreInteractions(mockedInputMapper);

        verifyNoInteractions(eventStore, emitter);
    }

    @Test
    void shouldReturnErrorWhenOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageMapper<TilesRendered, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(TilesRendered.class))).thenThrow(exception);

        final var design = theDefaultDesign();
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new BufferedTileRenderCompletedController(eventStore, inputMapper, mockedOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(someInputMessages()).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(mockedOutputMapper).transform(any(TilesRendered.class));
        verifyNoMoreInteractions(mockedOutputMapper);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var design = theDefaultDesign();
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new BufferedTileRenderCompletedController(eventStore, inputMapper, outputMapper, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(someInputMessages()).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(mockedEmitter).send(any(OutputMessage.class));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenMessagesAreInvalid() {
        final var design = theDefaultDesign();
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        assertThatThrownBy(() -> controller.onNext(someInvalidInputMessages()).toCompletable().await()).isInstanceOf(IllegalArgumentException.class);

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    private static Stream<Arguments> someMessages() {
        return Stream.of(
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", 3, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusHours(1)),
                        List.of(
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "CREATED", 0, 0, 0))
                        ),
                        TilesRenderedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTilesRenderedEvent(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, List.of(
                                Tile.builder().withLevel(0).withRow(0).withCol(0).build()
                        )))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", 3, TilesBitmap.empty(), dateTime.minusHours(5), dateTime.minusHours(3)),
                        List.of(
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "UPDATED", 0, 0, 0)),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "UPDATED", 1, 1, 0)),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(1), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "UPDATED", 2, 2, 1))
                        ),
                        TilesRenderedFactory.createOutputMessage(DESIGN_ID_2, aMessageId(), aTilesRenderedEvent(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, List.of(
                                Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                                Tile.builder().withLevel(1).withRow(1).withCol(0).build(),
                                Tile.builder().withLevel(2).withRow(2).withCol(1).build()
                        )))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", 3, TilesBitmap.empty().putTile(4, 0, 0), dateTime.minusHours(5), dateTime.minusHours(3)),
                        List.of(
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "UPDATED", 0, 0, 0)),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "UPDATED", 1, 1, 0)),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(1), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_1, DATA_2, REVISION_1, "UPDATED", 2, 2, 1))
                        ),
                        TilesRenderedFactory.createOutputMessage(DESIGN_ID_2, aMessageId(), aTilesRenderedEvent(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, List.of(
                                Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                                Tile.builder().withLevel(1).withRow(1).withCol(0).build()
                        )))
                )
        );
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, UUID userId, String data, String revision, String status, int levels, TilesBitmap bitmap, LocalDateTime created, LocalDateTime updated) {
        return Design.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withData(data)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withStatus(status)
                .withLevels(levels)
                .withBitmap(bitmap.getBitmap())
                .withPublished(false)
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static TileRenderCompleted aTileRenderCompleted(UUID designId, UUID commandId, String data, String revision, String status, int level, int row, int col) {
        return TileRenderCompleted.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withStatus(status)
                .withLevel(level)
                .withRow(row)
                .withCol(col)
                .build();
    }

    @NotNull
    private static TilesRendered aTilesRenderedEvent(UUID designId, UUID commandId, String data, String revision, List<Tile> tiles) {
        return TilesRendered.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withData(data)
                .withTiles(tiles)
                .build();
    }

    @NotNull
    private static Design theDefaultDesign() {
        return aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", LEVELS_DRAFT, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusHours(1));
    }

    @NotNull
    private static List<InputMessage> someInputMessages() {
        return List.of(
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "CREATED", 0, 0, 0)),
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "CREATED", 1, 0, 0))
        );
    }

    @NotNull
    private static List<InputMessage> someInvalidInputMessages() {
        return List.of(
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "CREATED", 0, 0, 0)),
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_0, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_1, DATA_1, REVISION_0, "CREATED", 1, 0, 0))
        );
    }

    private static class TileRenderCompletedFactory {
        @NotNull
        public static InputMessage createInputMessage(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted) {
            return TestUtils.createInputMessage(
                    designId.toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
            );
        }
    }

    private static class TilesRenderedFactory {
        @NotNull
        public static OutputMessage createOutputMessage(UUID designId, UUID messageId, TilesRendered tilesRenderedEvent) {
            return TestUtils.createOutputMessage(
                    designId.toString(), TILES_RENDERED, messageId, tilesRenderedEvent
            );
        }
    }
}