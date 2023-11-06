package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdatedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TilesRenderedInputMapper;
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
    private static final Mapper<InputMessage, TilesRendered> inputMapper = new TilesRenderedInputMapper();
    private static final MessageMapper<DesignAggregateUpdated, OutputMessage> outputMapper = new DesignAggregateUpdatedOutputMapper(MESSAGE_SOURCE);

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter emitter = mock();
    private final DesignEventStore eventStore = mock();

    private final TilesRenderedController controller = new TilesRenderedController(eventStore, inputMapper, outputMapper, emitter);

    private static final TilesBitmap bitmap0 = TilesBitmap.empty();
    private static final TilesBitmap bitmap1 = TilesBitmap.empty().putTile(0, 0, 0);
    private static final TilesBitmap bitmap2 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0);
    private static final TilesBitmap bitmap3 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0).putTile(2, 2, 1);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldPublishAMessageToInformThatTheDesignAggregateHasChanged(Design design, String revision, InputMessage inputMessage, OutputMessage expectedOutputMessage) {
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(design.getDesignId(), revision)).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(design.getDesignId(), revision);
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
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
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
        final RuntimeException exception = new RuntimeException();
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).appendMessage(inputMessage);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenAppendMessageFails() {
        final RuntimeException exception = new RuntimeException();
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
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
        final RuntimeException exception = new RuntimeException();
        final Design design = theDefaultDesign(DESIGN_ID_1, REVISION_0);
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
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
    void shouldReturnErrorWhenInputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final Mapper<InputMessage, TilesRendered> mockedInputMapper = mock();
        when(mockedInputMapper.transform(any(InputMessage.class))).thenThrow(exception);

        final Design design = theDefaultDesign(DESIGN_ID_1, REVISION_0);
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.error(exception));

        final var controller = new TilesRenderedController(eventStore, mockedInputMapper, outputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(mockedInputMapper).transform(any(InputMessage.class));
        verifyNoMoreInteractions(mockedInputMapper);

        verify(eventStore).appendMessage(inputMessage);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageMapper<DesignAggregateUpdated, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(DesignAggregateUpdated.class))).thenThrow(exception);

        final Design design = theDefaultDesign(DESIGN_ID_1, REVISION_0);
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TilesRenderedController(eventStore, inputMapper, mockedOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).appendMessage(inputMessage);
        verify(eventStore).projectDesign(DESIGN_ID_1, REVISION_0);
        verify(eventStore).updateDesign(design);
        verifyNoMoreInteractions(eventStore);

        verify(mockedOutputMapper).transform(any(DesignAggregateUpdated.class));
        verifyNoMoreInteractions(mockedOutputMapper);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final Design design = theDefaultDesign(DESIGN_ID_1, REVISION_0);
        final InputMessage inputMessage = anInputMessage(DESIGN_ID_1, REVISION_0);
        when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
        when(eventStore.projectDesign(DESIGN_ID_1, REVISION_0)).thenReturn(Single.just(Optional.of(design)));
        when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TilesRenderedController(eventStore, inputMapper, outputMapper, mockedEmitter);

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
                        REVISION_1,
                        TilesRenderedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_1, dateTime.minusMinutes(3), aTilesRendered(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, List.of(
                                Tile.builder().withLevel(0).withRow(0).withCol(0).build()
                        ))),
                        DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), getDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, REVISION_1, DATA_1, LEVELS_DRAFT, bitmap1, false, "CREATED"))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_2, "UPDATED", LEVELS_DRAFT, bitmap2, dateTime.minusHours(2), dateTime.minusMinutes(3)),
                        REVISION_2,
                        TilesRenderedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_2, dateTime.minusMinutes(3), aTilesRendered(DESIGN_ID_1, COMMAND_ID_1, REVISION_2, DATA_1, List.of(
                                Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                                Tile.builder().withLevel(1).withRow(1).withCol(0).build()
                        ))),
                        DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), getDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, REVISION_2, DATA_1, LEVELS_DRAFT, bitmap2, false, "UPDATED"))
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_2, "UPDATED", LEVELS_READY, bitmap3, dateTime.minusHours(2), dateTime.minusMinutes(3)),
                        REVISION_2,
                        TilesRenderedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_2, dateTime.minusMinutes(3), aTilesRendered(DESIGN_ID_2, COMMAND_ID_2, REVISION_2, DATA_2, List.of(
                                Tile.builder().withLevel(0).withRow(0).withCol(0).build(),
                                Tile.builder().withLevel(1).withRow(1).withCol(0).build(),
                                Tile.builder().withLevel(2).withRow(2).withCol(1).build()
                        ))),
                        DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_2, aMessageId(), getDesignAggregateUpdated(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, REVISION_2, DATA_2, LEVELS_READY, bitmap3, true, "UPDATED"))
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
                .withPublished(levels == LEVELS_READY)
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static TilesRendered aTilesRendered(UUID designId, UUID commandId, String revision, String data, List<Tile> tiles) {
        return TilesRendered.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withRevision(revision)
                .withData(data)
                .withChecksum(Checksum.of(data))
                .withTiles(tiles)
                .build();
    }

    @NotNull
    private static DesignAggregateUpdated getDesignAggregateUpdated(UUID designId, UUID commandId, UUID userId, String revision, String data, int levels, TilesBitmap bitmap, boolean published, String status) {
        return DesignAggregateUpdated.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withRevision(revision)
                .withData(data)
                .withChecksum(Checksum.of(data))
                .withLevels(levels)
                .withBitmap(bitmap.getBitmap())
                .withPublished(published)
                .withStatus(status)
                .withCreated(dateTime.minusHours(2))
                .withUpdated(dateTime.minusMinutes(3))
                .build();
    }

    @NotNull
    private static Design theDefaultDesign(UUID designId, String revision) {
        return aDesign(designId, COMMAND_ID_1, USER_ID_1, DATA_1, revision, "CREATED", LEVELS_DRAFT, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusHours(1));
    }

    @NotNull
    private InputMessage anInputMessage(UUID designId, String revision) {
        return TilesRenderedFactory.createInputMessage(designId, aMessageId(), revision, dateTime.minusMinutes(3), aTilesRendered(designId, COMMAND_ID_1, revision, DATA_1, List.of(Tile.builder().withLevel(0).withRow(0).withCol(0).build())));
    }

    private static class TilesRenderedFactory {
        @NotNull
        public static InputMessage createInputMessage(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, TilesRendered tilesRendered) {
            return TestUtils.createInputMessage(
                    designId.toString(), TILES_RENDERED, messageId, tilesRendered, messageToken, messageTime
            );
        }
    }

    private static class DesignAggregateUpdatedFactory {
        @NotNull
        public static OutputMessage createOutputMessage(UUID designId, UUID messageId, DesignAggregateUpdated designAggregateUpdated) {
            return TestUtils.createOutputMessage(
                    designId.toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated
            );
        }
    }
}