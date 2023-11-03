package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TileRenderCompletedControllerTest {
    private static final Mapper<InputMessage, TileRenderCompleted> inputMapper = new TileRenderCompletedInputMapper();
    private static final MessageMapper<TileRenderCompleted, OutputMessage> bufferOutputMapper = new TileRenderCompletedOutputMapper(MESSAGE_SOURCE);
    private static final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper = new TileRenderRequestedOutputMapper(MESSAGE_SOURCE);

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter bufferEmitter = mock();
    private final MessageEmitter renderEmitter = mock();
    private final DesignEventStore eventStore = mock();

    private final TileRenderCompletedController controller = new TileRenderCompletedController(eventStore, inputMapper, bufferOutputMapper, renderOutputMapper, bufferEmitter, renderEmitter);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldHandleMessage(Design design, InputMessage inputMessage, OutputMessage expectedOutputMessage1, OutputMessage expectedOutputMessage2, String topicName) {
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(renderEmitter.getTopicName()).thenReturn("render");
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));
        when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(bufferEmitter).send(assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage1.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage1.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage1.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage1.getValue().getSource());
        }));
        verifyNoMoreInteractions(bufferEmitter);

        verify(renderEmitter).getTopicName();
        verify(renderEmitter).send(assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage2.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage2.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage2.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage2.getValue().getSource());
        }), eq(topicName));
        verifyNoMoreInteractions(renderEmitter);
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
                                .withLevels(8)
                                .withBitmap(TilesBitmap.empty().getBitmap())
                                .withPublished(true)
                                .withCreated(dateTime.minusHours(2))
                                .withUpdated(dateTime.minusHours(1))
                                .build(),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, UUID.randomUUID(), REVISION_0, dateTime.minusMinutes(3), TileRenderCompleted.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withStatus("CREATED")
                                .withLevel(4)
                                .withRow(0)
                                .withCol(0)
                                .build()),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, UUID.randomUUID(), TileRenderCompleted.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withStatus("CREATED")
                                .withLevel(4)
                                .withRow(0)
                                .withCol(0)
                                .build()),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, UUID.randomUUID(), TileRenderRequested.builder()
                                .withDesignId(DESIGN_ID_1)
                                .withCommandId(COMMAND_ID_1)
                                .withData(DATA_1)
                                .withChecksum(Checksum.of(DATA_1))
                                .withRevision(REVISION_0)
                                .withLevel(4)
                                .withRow(0)
                                .withCol(1)
                                .build()),
                        "render-requested-1"
                )
        );
    }

    private static class TileRenderCompletedFactory {
        @NotNull
        public static InputMessage createInputMessage(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted) {
            return TestUtils.createInputMessage(
                    designId.toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
            );
        }

        @NotNull
        public static OutputMessage createOutputMessage(UUID designId, UUID messageId, TileRenderCompleted tileRenderCompleted) {
            return TestUtils.createOutputMessage(
                    designId.toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
            );
        }
    }

    private static class TileRenderRequestedFactory {
        @NotNull
        public static OutputMessage createOutputMessage(UUID designId, UUID messageId, TileRenderRequested tileRenderRequested) {
            return TestUtils.createOutputMessage(
                    designId.toString(), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
            );
        }
    }
}