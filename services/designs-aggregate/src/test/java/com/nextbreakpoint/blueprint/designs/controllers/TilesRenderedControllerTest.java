package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.designs.TestConstants;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import com.nextbreakpoint.blueprint.designs.TestUtils;
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
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_AGGREGATE_UPDATED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILES_RENDERED;
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

class TilesRenderedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter<DesignAggregateUpdated> emitter = mock();
    private final DesignEventStore eventStore = mock();

    private final TilesRenderedController controller = new TilesRenderedController(MESSAGE_SOURCE, eventStore, emitter);

    private static final Bitmap bitmap1 = Bitmap.empty().putTile(0, 0, 0);
    private static final Bitmap bitmap2 = Bitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0);
    private static final Bitmap bitmap3 = Bitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0).putTile(2, 2, 1);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldPublishAMessageToInformThatTheDesignAggregateHasChanged(Design design, InputMessage<TilesRendered> inputMessage, OutputMessage<TilesRendered> expectedOutputMessage) {
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(design.getDesignId(), inputMessage.getToken())).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(design.getDesignId(), inputMessage.getToken());
        verify(eventStore).updateDesign(design);
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
    void shouldDoNothingIfDesignDoesNotExists() {
        final InputMessage<TilesRendered> inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.just(Optional.empty()));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(DESIGN_ID_1, REVISION_0);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenProjectDesignFails() {
        final var exception = new RuntimeException();
        final InputMessage<TilesRendered> inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).appendMessage(inputMessage);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenAppendMessageFails() {
        final var exception = new RuntimeException();
        final InputMessage<TilesRendered> inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(DESIGN_ID_1, REVISION_0);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenUpdateDesignFails() {
        final var exception = new RuntimeException();
        final Design design = theDefaultDesign(DESIGN_ID_1, REVISION_0);
        final InputMessage<TilesRendered> inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(DESIGN_ID_1, REVISION_0);
        verify(eventStore).updateDesign(design);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final var exception = new RuntimeException();
        final MessageEmitter<DesignAggregateUpdated> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final Design design = theDefaultDesign(DESIGN_ID_1, REVISION_0);
        final InputMessage<TilesRendered> inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TilesRenderedController(MESSAGE_SOURCE, eventStore, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(DESIGN_ID_1, REVISION_0);
        verify(eventStore).updateDesign(design);
        verifyNoMoreInteractions(eventStore);

        verify(mockedEmitter).send(any(OutputMessage.class));
        verifyNoMoreInteractions(mockedEmitter);
    }

    private static Stream<Arguments> someMessages() {
        return Stream.of(
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_1, "CREATED", LEVELS_DRAFT, bitmap1, dateTime.minusHours(2), dateTime.minusMinutes(3)),
                        TestFactory.createInputMessage(aMessageId(), REVISION_1, dateTime.minusMinutes(3), aTilesRendered(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, List.of(
                                Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build()
                        ))),
                        TestFactory.createOutputMessage(aMessageId(), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, REVISION_1, DATA_1, LEVELS_DRAFT, bitmap1, false, "CREATED", dateTime.minusHours(2), dateTime.minusMinutes(3)))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_2, "UPDATED", LEVELS_DRAFT, bitmap2, dateTime.minusHours(2), dateTime.minusMinutes(3)),
                        TestFactory.createInputMessage(aMessageId(), REVISION_2, dateTime.minusMinutes(3), aTilesRendered(DESIGN_ID_1, COMMAND_ID_1, REVISION_2, DATA_1, List.of(
                                Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build(),
                                Tile.newBuilder().setLevel(1).setRow(1).setCol(0).build()
                        ))),
                        TestFactory.createOutputMessage(aMessageId(), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, REVISION_2, DATA_1, LEVELS_DRAFT, bitmap2, false, "UPDATED", dateTime.minusHours(2), dateTime.minusMinutes(3)))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_2, "UPDATED", LEVELS_READY, bitmap3, dateTime.minusHours(3), dateTime.minusMinutes(1)),
                        TestFactory.createInputMessage(aMessageId(), REVISION_2, dateTime.minusMinutes(1), aTilesRendered(DESIGN_ID_2, COMMAND_ID_2, REVISION_2, DATA_2, List.of(
                                Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build(),
                                Tile.newBuilder().setLevel(1).setRow(1).setCol(0).build(),
                                Tile.newBuilder().setLevel(2).setRow(2).setCol(1).build()
                        ))),
                        TestFactory.createOutputMessage(aMessageId(), aDesignAggregateUpdated(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, REVISION_2, DATA_2, LEVELS_READY, bitmap3, true, "UPDATED", dateTime.minusHours(3), dateTime.minusMinutes(1)))
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
                .withPublished(levels == LEVELS_READY)
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static TilesRendered aTilesRendered(UUID designId, UUID commandId, String revision, String data, List<Tile> tiles) {
        return TilesRendered.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .setData(data)
                .setChecksum(Checksum.of(data))
                .setTiles(tiles)
                .build();
    }

    @NotNull
    private static DesignAggregateUpdated aDesignAggregateUpdated(UUID designId, UUID commandId, UUID userId, String revision, String data, int levels, Bitmap bitmap, boolean published, String status, LocalDateTime created, LocalDateTime updated) {
        return DesignAggregateUpdated.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setRevision(revision)
                .setData(data)
                .setChecksum(Checksum.of(data))
                .setLevels(levels)
                .setBitmap(bitmap.toByteBuffer())
                .setPublished(published)
                .setStatus(DesignAggregateStatus.valueOf(status))
                .setCreated(created.toInstant(ZoneOffset.UTC))
                .setUpdated(updated.toInstant(ZoneOffset.UTC))
                .build();
    }

    @NotNull
    private static Design theDefaultDesign(UUID designId, String revision) {
        return aDesign(designId, COMMAND_ID_1, USER_ID_1, DATA_1, revision, "CREATED", LEVELS_DRAFT, Bitmap.empty(), dateTime.minusHours(2), dateTime.minusHours(1));
    }

    @NotNull
    private InputMessage<TilesRendered> anInputMessage(UUID designId, String revision) {
        return TestFactory.createInputMessage(aMessageId(), revision, dateTime.minusMinutes(3), aTilesRendered(designId, COMMAND_ID_1, revision, DATA_1, List.of(Tile.newBuilder().setLevel(0).setRow(0).setCol(0).build())));
    }
}