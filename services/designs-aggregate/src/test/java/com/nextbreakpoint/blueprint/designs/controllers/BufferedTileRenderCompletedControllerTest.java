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
    void shouldHandleMessages(Design design, List<InputMessage> inputMessages, OutputMessage expectedOutputMessage) {
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
    void shouldDoNothingWhenDesignDoesNoExist() {
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
        verifyNoInteractions(eventStore, emitter);
    }

    @Test
    void shouldReturnErrorWhenOutputMapperFails() {
        final var design = theDefaultDesign();

        final RuntimeException exception = new RuntimeException();
        final MessageMapper<TilesRendered, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(TilesRendered.class))).thenThrow(exception);

        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new BufferedTileRenderCompletedController(eventStore, inputMapper, mockedOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(someInputMessages()).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(mockedOutputMapper).transform(any(TilesRendered.class));
        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final var design = theDefaultDesign();

        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new BufferedTileRenderCompletedController(eventStore, inputMapper, outputMapper, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(someInputMessages()).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(mockedEmitter).send(any(OutputMessage.class));
    }

    @Test
    void shouldReturnErrorWhenMessagesAreInvalid() {
        final var design = theDefaultDesign();

        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        assertThatThrownBy(() -> controller.onNext(invalidInputMessages()).toCompletable().await()).isInstanceOf(IllegalArgumentException.class);

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);
        verifyNoInteractions(emitter);
    }

    private static Stream<Arguments> someMessages() {
        return Stream.of(
                Arguments.of(
                        Design.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withUserId(USER_ID_1)
                                .withData(DATA_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withStatus("CREATED")
                                .withLevels(3)
                                .withBitmap(TilesBitmap.empty().getBitmap())
                                .withPublished(false)
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build(),
                        List.of(
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), TileRenderCompleted.builder()
                                    .withDesignId(DESIGN_ID_1)
                                    .withCommandId(COMMAND_ID_1)
                                    .withChecksum(Checksum.of(DATA_1))
                                    .withRevision(REVISION_0)
                                    .withStatus("CREATED")
                                    .withLevel(0)
                                    .withRow(0)
                                    .withCol(0)
                                    .build())
                        ),
                        TilesRenderedFactory.createOutputMessage(DESIGN_ID_1, UUID.randomUUID(), TilesRendered.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withData(DATA_1)
                                .withTiles(List.of(
                                        Tile.builder().withLevel(0).withRow(0).withCol(0).build()
                                ))
                                .build())
                ),
                Arguments.of(
                        Design.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withUserId(USER_ID_2)
                                .withData(DATA_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withStatus("UPDATED")
                                .withLevels(3)
                                .withBitmap(TilesBitmap.empty().getBitmap())
                                .withPublished(false)
                                .withCreated(dateTime.minusHours(5))
                                .withUpdated(dateTime.minusHours(3))
                                .build(),
                        List.of(
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(3), TileRenderCompleted.builder()
                                        .withDesignId(DESIGN_ID_2)
                                        .withCommandId(COMMAND_ID_2)
                                        .withChecksum(Checksum.of(DATA_2))
                                        .withRevision(REVISION_1)
                                        .withStatus("UPDATED")
                                        .withLevel(0)
                                        .withRow(0)
                                        .withCol(0)
                                        .build()),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(2), TileRenderCompleted.builder()
                                        .withDesignId(DESIGN_ID_2)
                                        .withCommandId(COMMAND_ID_2)
                                        .withChecksum(Checksum.of(DATA_2))
                                        .withRevision(REVISION_1)
                                        .withStatus("UPDATED")
                                        .withLevel(1)
                                        .withRow(1)
                                        .withCol(0)
                                        .build()),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(1), TileRenderCompleted.builder()
                                        .withDesignId(DESIGN_ID_2)
                                        .withCommandId(COMMAND_ID_2)
                                        .withChecksum(Checksum.of(DATA_2))
                                        .withRevision(REVISION_1)
                                        .withStatus("UPDATED")
                                        .withLevel(2)
                                        .withRow(2)
                                        .withCol(1)
                                        .build())
                        ),
                        TilesRenderedFactory.createOutputMessage(DESIGN_ID_2, UUID.randomUUID(), TilesRendered.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withData(DATA_2)
                                .withTiles(List.of(
                                        Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                                        Tile.builder().withLevel(1).withRow(1).withCol(0).build(),
                                        Tile.builder().withLevel(2).withRow(2).withCol(1).build()
                                ))
                                .build())
                ),
                Arguments.of(
                        Design.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withUserId(USER_ID_2)
                                .withData(DATA_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withStatus("UPDATED")
                                .withLevels(3)
                                .withBitmap(TilesBitmap.empty().putTile(4, 0, 0).getBitmap())
                                .withPublished(false)
                                .withCreated(dateTime.minusHours(5))
                                .withUpdated(dateTime.minusHours(3))
                                .build(),
                        List.of(
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(3), TileRenderCompleted.builder()
                                        .withDesignId(DESIGN_ID_2)
                                        .withCommandId(COMMAND_ID_2)
                                        .withChecksum(Checksum.of(DATA_2))
                                        .withRevision(REVISION_1)
                                        .withStatus("UPDATED")
                                        .withLevel(0)
                                        .withRow(0)
                                        .withCol(0)
                                        .build()),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(2), TileRenderCompleted.builder()
                                        .withDesignId(DESIGN_ID_2)
                                        .withCommandId(COMMAND_ID_2)
                                        .withChecksum(Checksum.of(DATA_2))
                                        .withRevision(REVISION_1)
                                        .withStatus("UPDATED")
                                        .withLevel(1)
                                        .withRow(1)
                                        .withCol(0)
                                        .build()),
                                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(1), TileRenderCompleted.builder()
                                        .withDesignId(DESIGN_ID_2)
                                        .withCommandId(COMMAND_ID_1)
                                        .withChecksum(Checksum.of(DATA_2))
                                        .withRevision(REVISION_1)
                                        .withStatus("UPDATED")
                                        .withLevel(2)
                                        .withRow(2)
                                        .withCol(1)
                                        .build())
                        ),
                        TilesRenderedFactory.createOutputMessage(DESIGN_ID_2, UUID.randomUUID(), TilesRendered.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withData(DATA_2)
                                .withTiles(List.of(
                                        Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                                        Tile.builder().withLevel(1).withRow(1).withCol(0).build()
                                ))
                                .build())
                )
        );
    }

    @NotNull
    private static Design theDefaultDesign() {
        return Design.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withUserId(USER_ID_1)
                .withData(DATA_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(REVISION_0)
                .withStatus("CREATED")
                .withLevels(3)
                .withBitmap(TilesBitmap.empty().getBitmap())
                .withPublished(false)
                .withCreated(dateTime.minusHours(2))
                .withUpdated(dateTime.minusHours(1))
                .build();
    }

    @NotNull
    private static List<InputMessage> someInputMessages() {
        return List.of(
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), TileRenderCompleted.builder()
                        .withDesignId(DESIGN_ID_1)
                        .withCommandId(COMMAND_ID_1)
                        .withChecksum(Checksum.of(DATA_1))
                        .withRevision(REVISION_0)
                        .withStatus("CREATED")
                        .withLevel(0)
                        .withRow(0)
                        .withCol(0)
                        .build()),
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(2), TileRenderCompleted.builder()
                        .withDesignId(DESIGN_ID_1)
                        .withCommandId(COMMAND_ID_1)
                        .withChecksum(Checksum.of(DATA_1))
                        .withRevision(REVISION_0)
                        .withStatus("CREATED")
                        .withLevel(1)
                        .withRow(1)
                        .withCol(0)
                        .build())
        );
    }

    @NotNull
    private static List<InputMessage> invalidInputMessages() {
        return List.of(
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), TileRenderCompleted.builder()
                        .withDesignId(DESIGN_ID_1)
                        .withCommandId(COMMAND_ID_1)
                        .withChecksum(Checksum.of(DATA_1))
                        .withRevision(REVISION_0)
                        .withStatus("CREATED")
                        .withLevel(0)
                        .withRow(0)
                        .withCol(0)
                        .build()),
                TileRenderCompletedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(2), TileRenderCompleted.builder()
                        .withDesignId(DESIGN_ID_2)
                        .withCommandId(COMMAND_ID_1)
                        .withChecksum(Checksum.of(DATA_1))
                        .withRevision(REVISION_0)
                        .withStatus("CREATED")
                        .withLevel(1)
                        .withRow(1)
                        .withCol(0)
                        .build())
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