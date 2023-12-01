package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileStatus;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
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
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter<TilesRendered> emitter = mock();
    private final DesignEventStore eventStore = mock();

    private final BufferedTileRenderCompletedController controller = new BufferedTileRenderCompletedController(MESSAGE_SOURCE, eventStore, emitter);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldPublishAMessageToInformThatTilesHaveBeingRendered(Design design, List<InputMessage<TileRenderCompleted>> inputMessages, OutputMessage<Object> expectedOutputMessage) {
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
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter<TilesRendered> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var design = theDefaultDesign();
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new BufferedTileRenderCompletedController(MESSAGE_SOURCE, eventStore, mockedEmitter);

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
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", 3, Bitmap.empty(), dateTime.minusHours(2), dateTime.minusHours(1)),
                        List.of(
                                TestFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "COMPLETED", 0, 0, 0))
                        ),
                        TestFactory.createOutputMessage(aMessageId(), aTilesRenderedEvent(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, List.of(
                                Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build()
                        )))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", 3, Bitmap.empty(), dateTime.minusHours(5), dateTime.minusHours(3)),
                        List.of(
                                TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "COMPLETED", 0, 0, 0)),
                                TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "COMPLETED", 1, 1, 0)),
                                TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(1), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "FAILED", 2, 2, 1))
                        ),
                        TestFactory.createOutputMessage(aMessageId(), aTilesRenderedEvent(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, List.of(
                                Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build(),
                                Tile.newBuilder().setLevel(1).setRow(1).setCol(0).build(),
                                Tile.newBuilder().setLevel(2).setRow(2).setCol(1).build()
                        )))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", 3, Bitmap.empty().putTile(4, 0, 0), dateTime.minusHours(5), dateTime.minusHours(3)),
                        List.of(
                                TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "COMPLETED", 0, 0, 0)),
                                TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, "FAILED", 1, 1, 0)),
                                TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(1), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_1, DATA_2, REVISION_1, "COMPLETED", 2, 2, 1))
                        ),
                        TestFactory.createOutputMessage(aMessageId(), aTilesRenderedEvent(DESIGN_ID_2, COMMAND_ID_2, DATA_2, REVISION_1, List.of(
                                Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build(),
                                Tile.newBuilder().setLevel(1).setRow(1).setCol(0).build()
                        )))
                )
        );
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, UUID userId, String data, String revision, String status, int levels, Bitmap bitmap, LocalDateTime created, LocalDateTime updated) {
        return Design.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withData(data)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withStatus(status)
                .withLevels(levels)
                .withBitmap(bitmap.toByteBuffer())
                .withPublished(false)
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static TileRenderCompleted aTileRenderCompleted(UUID designId, UUID commandId, String data, String revision, String status, int level, int row, int col) {
        return TileRenderCompleted.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setChecksum(Checksum.of(data))
                .setRevision(revision)
                .setStatus(TileStatus.valueOf(status))
                .setLevel(level)
                .setRow(row)
                .setCol(col)
                .build();
    }

    @NotNull
    private static TilesRendered aTilesRenderedEvent(UUID designId, UUID commandId, String data, String revision, List<Tile> tiles) {
        return TilesRendered.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setChecksum(Checksum.of(data))
                .setRevision(revision)
                .setData(data)
                .setTiles(tiles)
                .build();
    }

    @NotNull
    private static Design theDefaultDesign() {
        return aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", LEVELS_DRAFT, Bitmap.empty(), dateTime.minusHours(2), dateTime.minusHours(1));
    }

    @NotNull
    private static List<InputMessage<TileRenderCompleted>> someInputMessages() {
        return List.of(
                TestFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "COMPLETED", 0, 0, 0)),
                TestFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "COMPLETED", 1, 0, 0))
        );
    }

    @NotNull
    private static List<InputMessage<TileRenderCompleted>> someInvalidInputMessages() {
        return List.of(
                TestFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, DATA_1, REVISION_0, "COMPLETED", 0, 0, 0)),
                TestFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(2), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_1, DATA_1, REVISION_0, "COMPLETED", 1, 0, 0))
        );
    }
}