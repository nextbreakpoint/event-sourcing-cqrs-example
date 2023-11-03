package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
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
    private static final MessageMapper<DesignDocumentUpdateRequested, OutputMessage> updateOutputMapper = new DesignDocumentUpdateRequestedOutputMapper(MESSAGE_SOURCE);
    private static final MessageMapper<DesignDocumentDeleteRequested, OutputMessage> deleteOutputMapper = new DesignDocumentDeleteRequestedOutputMapper(MESSAGE_SOURCE);

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    public static final TilesBitmap bitmap0 = TilesBitmap.empty();
    public static final TilesBitmap bitmap1 = TilesBitmap.empty().putTile(0, 0, 0);
    public static final TilesBitmap bitmap2 = TilesBitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0);

    private final MessageEmitter emitter = mock();

    private final DesignAggregateUpdatedController controller = new DesignAggregateUpdatedController(inputMapper, updateOutputMapper, deleteOutputMapper, emitter);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldHandleMessage(InputMessage inputMessage, OutputMessage expectedOutputMessage) {
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

        assertThatThrownBy(() -> controller.onNext(someUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedInputMapper).transform(any(InputMessage.class));
        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenUpdateOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageMapper<DesignDocumentUpdateRequested, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(DesignDocumentUpdateRequested.class))).thenThrow(exception);

        final var controller = new DesignAggregateUpdatedController(inputMapper, mockedOutputMapper, deleteOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(someUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedOutputMapper).transform(any(DesignDocumentUpdateRequested.class));
        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenDeleteOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageMapper<DesignDocumentDeleteRequested, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(DesignDocumentDeleteRequested.class))).thenThrow(exception);

        final var controller = new DesignAggregateUpdatedController(inputMapper, updateOutputMapper, mockedOutputMapper, emitter);

        assertThatThrownBy(() -> controller.onNext(someDeleteInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedOutputMapper).transform(any(DesignDocumentDeleteRequested.class));
        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var controller = new DesignAggregateUpdatedController(inputMapper, updateOutputMapper, deleteOutputMapper, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(someUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).send(any(OutputMessage.class));
    }

    private static Stream<Arguments> someMessages() {
        return Stream.of(
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), DesignAggregateUpdated.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withUserId(USER_ID_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withStatus("CREATED")
                                .withData(DATA_1)
                                .withPublished(false)
                                .withLevels(LEVELS_DRAFT)
                                .withBitmap(bitmap0.getBitmap())
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build()),
                        DesignDocumentUpdateRequestedFactory.createInputMessage(DESIGN_ID_1, UUID.randomUUID(), DesignDocumentUpdateRequested.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withUserId(USER_ID_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withStatus("CREATED")
                                .withData(DATA_1)
                                .withPublished(false)
                                .withLevels(LEVELS_DRAFT)
                                .withTiles(toTiles(bitmap0))
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build())
                ),
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(DESIGN_ID_2, UUID.randomUUID(), REVISION_1, dateTime.minusMinutes(3), DesignAggregateUpdated.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withUserId(USER_ID_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withStatus("UPDATED")
                                .withData(DATA_2)
                                .withPublished(true)
                                .withLevels(LEVELS_READY)
                                .withBitmap(bitmap1.getBitmap())
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build()),
                        DesignDocumentUpdateRequestedFactory.createInputMessage(DESIGN_ID_2, UUID.randomUUID(), DesignDocumentUpdateRequested.builder()
                                .withDesignId(DESIGN_ID_2)
                                .withCommandId(COMMAND_ID_2)
                                .withUserId(USER_ID_2)
                                .withChecksum(Checksum.of(DATA_2))
                                .withRevision(REVISION_1)
                                .withStatus("UPDATED")
                                .withData(DATA_2)
                                .withPublished(true)
                                .withLevels(LEVELS_READY)
                                .withTiles(toTiles(bitmap1))
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build())
                ),
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(DESIGN_ID_3, UUID.randomUUID(), REVISION_2, dateTime.minusMinutes(3), DesignAggregateUpdated.builder()
                                .withDesignId(DESIGN_ID_3)
                                .withCommandId(COMMAND_ID_3)
                                .withUserId(USER_ID_1)
                                .withChecksum(Checksum.of(DATA_3))
                                .withRevision(REVISION_2)
                                .withStatus("DELETED")
                                .withData(DATA_3)
                                .withPublished(false)
                                .withLevels(LEVELS_DRAFT)
                                .withBitmap(bitmap2.getBitmap())
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build()),
                        DesignDocumentDeleteRequestedFactory.createOutputMessage(DESIGN_ID_3, UUID.randomUUID(), DesignDocumentDeleteRequested.builder()
                                .withDesignId(DESIGN_ID_3)
                                .withCommandId(COMMAND_ID_3)
                                .withRevision(REVISION_2)
                                .build())
                )
        );
    }

    private InputMessage someUpdateInputMessage() {
        return DesignAggregateUpdatedFactory.of(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), DesignAggregateUpdated.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withUserId(USER_ID_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(REVISION_0)
                .withStatus("CREATED")
                .withData(DATA_1)
                .withPublished(false)
                .withLevels(LEVELS_DRAFT)
                .withBitmap(bitmap0.getBitmap())
                .withCreated(dateTime.minusHours(2))
                .withUpdated(dateTime.minusHours(1))
                .build());
    }

    private InputMessage someDeleteInputMessage() {
        return DesignAggregateUpdatedFactory.of(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), DesignAggregateUpdated.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withUserId(USER_ID_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(REVISION_0)
                .withStatus("DELETED")
                .withData(DATA_1)
                .withPublished(false)
                .withLevels(LEVELS_DRAFT)
                .withBitmap(bitmap0.getBitmap())
                .withCreated(dateTime.minusHours(2))
                .withUpdated(dateTime.minusHours(1))
                .build());
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