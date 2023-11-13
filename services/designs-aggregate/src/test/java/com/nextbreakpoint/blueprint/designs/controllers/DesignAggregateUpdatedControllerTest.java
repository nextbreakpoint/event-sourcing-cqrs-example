package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdatedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.designs.TestUtils;
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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_AGGREGATE_UPDATED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
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

class DesignAggregateUpdatedControllerTest {
    private static final Mapper<InputMessage, DesignAggregateUpdated> inputMapper = new DesignAggregateUpdatedInputMapper();
    private static final Mapper<DesignDocumentUpdateRequested, OutputMessage> updateOutputMapper = new DesignDocumentUpdateRequestedOutputMapper(MESSAGE_SOURCE);
    private static final Mapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper = new DesignDocumentDeleteRequestedOutputMapper(MESSAGE_SOURCE);

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private static final TilesBitmap bitmap0 = TilesBitmap.empty();
    private static final TilesBitmap bitmap1 = TilesBitmap.empty().putTile(0, 0, 0);
    private static final TilesBitmap bitmap2 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0);

    private final MessageEmitter emitter = mock();

    private final DesignAggregateUpdatedController controller = new DesignAggregateUpdatedController(inputMapper, updateOutputMapper, deleteOutputMapper, emitter);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldPublishAMessageToInformThatADocumentHasChanged(InputMessage inputMessage, OutputMessage expectedOutputMessage) {
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

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
    void shouldReturnErrorWhenInputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final Mapper<InputMessage, DesignAggregateUpdated> mockedInputMapper = mock();
        when(mockedInputMapper.transform(any(InputMessage.class))).thenThrow(exception);

        final var controller = new DesignAggregateUpdatedController(mockedInputMapper, updateOutputMapper, deleteOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(anUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedInputMapper).transform(any(InputMessage.class));
        verifyNoMoreInteractions(mockedInputMapper);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenUpdateOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final Mapper<DesignDocumentUpdateRequested, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(DesignDocumentUpdateRequested.class))).thenThrow(exception);

        final var controller = new DesignAggregateUpdatedController(inputMapper, mockedOutputMapper, deleteOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(anUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedOutputMapper).transform(any(DesignDocumentUpdateRequested.class));
        verifyNoMoreInteractions(mockedOutputMapper);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenDeleteOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final Mapper<DesignDocumentDeleteRequested, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(DesignDocumentDeleteRequested.class))).thenThrow(exception);

        final var controller = new DesignAggregateUpdatedController(inputMapper, updateOutputMapper, mockedOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(aDeleteInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedOutputMapper).transform(any(DesignDocumentDeleteRequested.class));
        verifyNoMoreInteractions(mockedOutputMapper);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var controller = new DesignAggregateUpdatedController(inputMapper, updateOutputMapper, deleteOutputMapper, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(anUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).send(any(OutputMessage.class));
        verifyNoMoreInteractions(emitter);
    }

    private static Stream<Arguments> someMessages() {
        return Stream.of(
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1))),
                        DesignDocumentUpdateRequestedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1)))
                ),
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(DESIGN_ID_2, aMessageId(), REVISION_1, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", true, LEVELS_READY, bitmap1, dateTime.minusHours(2), dateTime.minusHours(1))),
                        DesignDocumentUpdateRequestedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), aDesignDocumentUpdateRequested(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", true, LEVELS_READY, bitmap1, dateTime.minusHours(2), dateTime.minusHours(1)))
                ),
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(DESIGN_ID_3, aMessageId(), REVISION_2, dateTime.minusMinutes(1), aDesignAggregateUpdated(DESIGN_ID_3, COMMAND_ID_3, USER_ID_1, DATA_3, REVISION_2, "DELETED", false, LEVELS_DRAFT, bitmap2, dateTime.minusHours(2), dateTime.minusHours(1))),
                        DesignDocumentDeleteRequestedFactory.createOutputMessage(DESIGN_ID_3, aMessageId(), aDesignDocumentDeleteRequested(DESIGN_ID_3, COMMAND_ID_3, REVISION_2))
                )
        );
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static DesignAggregateUpdated aDesignAggregateUpdated(UUID designId, UUID commandId, UUID userId, String data, String revision, String CREATED, boolean published, int levels, TilesBitmap bitmap, LocalDateTime created, LocalDateTime updated) {
        return DesignAggregateUpdated.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withStatus(CREATED)
                .withData(data)
                .withPublished(published)
                .withLevels(levels)
                .withBitmap(bitmap.getBitmap())
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static DesignDocumentUpdateRequested aDesignDocumentUpdateRequested(UUID designId, UUID commandId, UUID userId, String data, String revision, String CREATED, boolean published, int levels, TilesBitmap bitmap, LocalDateTime created, LocalDateTime updated) {
        return DesignDocumentUpdateRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withStatus(CREATED)
                .withData(data)
                .withPublished(published)
                .withLevels(levels)
                .withTiles(toTiles(bitmap))
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static DesignDocumentDeleteRequested aDesignDocumentDeleteRequested(UUID designId, UUID commandId, String revision) {
        return DesignDocumentDeleteRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withRevision(revision)
                .build();
    }

    @NotNull
    private InputMessage anUpdateInputMessage() {
        return DesignAggregateUpdatedFactory.of(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1)));
    }

    @NotNull
    private InputMessage aDeleteInputMessage() {
        return DesignAggregateUpdatedFactory.of(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "DELETED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1)));
    }

    @NotNull
    private static List<Tiles> toTiles(TilesBitmap bitmap) {
        return IntStream.range(0, 8)
                .mapToObj(bitmap::toTiles)
                .collect(Collectors.toList());
    }

    private static class DesignAggregateUpdatedFactory {
        @NotNull
        public static InputMessage of(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, DesignAggregateUpdated designAggregateUpdated) {
            return TestUtils.createInputMessage(
                    designId.toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated, messageToken, messageTime
            );
        }
    }

    private static class DesignDocumentUpdateRequestedFactory {
        @NotNull
        public static OutputMessage createInputMessage(UUID designId, UUID messageId, DesignDocumentUpdateRequested designDocumentUpdateRequested) {
            return TestUtils.createOutputMessage(
                    designId.toString(), DESIGN_DOCUMENT_UPDATE_REQUESTED, messageId, designDocumentUpdateRequested
            );
        }
    }

    private static class DesignDocumentDeleteRequestedFactory {
        @NotNull
        public static OutputMessage createOutputMessage(UUID designId, UUID messageId, DesignDocumentDeleteRequested designDocumentDeleteRequested) {
            return TestUtils.createOutputMessage(
                    designId.toString(), DESIGN_DOCUMENT_DELETE_REQUESTED, messageId, designDocumentDeleteRequested
            );
        }
    }
}