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
import org.junit.jupiter.api.Test;
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
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @MethodSource("notTerminalMessages")
    void shouldPublishAMessageToInformThatATileHasBeenCompletedAndRequestTheRenderingOfTheNextTile(Design design, InputMessage inputMessage, OutputMessage expectedOutputMessage1, OutputMessage expectedOutputMessage2, String topicName) {
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

    @ParameterizedTest
    @MethodSource("terminalMessages")
    void shouldPublishAMessageToInformThatATileHasBeenCompletedButNotRequestTheRenderingOfAnotherTile(Design design, InputMessage inputMessage, OutputMessage expectedOutputMessage) {
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(renderEmitter.getTopicName()).thenReturn("render");
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));
        when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).findDesign(design.getDesignId());
        verifyNoMoreInteractions(eventStore);

        verify(bufferEmitter).send(assertArg(message -> {
            assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
            assertThat(message.getValue().getUuid()).isNotNull();
            assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
            assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
            assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        }));
        verifyNoMoreInteractions(bufferEmitter);

        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldDoNothingWhenDesignDoesNotExist() {
        when(eventStore.findDesign(DESIGN_ID_1)).thenReturn(Single.just(Optional.empty()));
        when(renderEmitter.getTopicName()).thenReturn("render");
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));
        when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

        controller.onNext(aInputMessage(4, 0, 0)).toCompletable().doOnError(Assertions::fail).await();

        verify(eventStore).findDesign(DESIGN_ID_1);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenFindDesignFails() {
        final RuntimeException exception = new RuntimeException();
        when(eventStore.findDesign(DESIGN_ID_1)).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(eventStore).findDesign(DESIGN_ID_1);
        verifyNoMoreInteractions(eventStore);

        verifyNoInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenInputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final Mapper<InputMessage, TileRenderCompleted> mockedInputMapper = mock();
        when(mockedInputMapper.transform(any(InputMessage.class))).thenThrow(exception);

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TileRenderCompletedController(eventStore, mockedInputMapper, bufferOutputMapper, renderOutputMapper, bufferEmitter, renderEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedInputMapper).transform(any(InputMessage.class));
        verifyNoMoreInteractions(mockedInputMapper);

        verifyNoInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenBufferOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageMapper<TileRenderCompleted, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(TileRenderCompleted.class))).thenThrow(exception);

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TileRenderCompletedController(eventStore, inputMapper, mockedOutputMapper, renderOutputMapper, bufferEmitter, renderEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedOutputMapper).transform(any(TileRenderCompleted.class));
        verifyNoMoreInteractions(mockedOutputMapper);

        verifyNoInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenRenderOutputMapperFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageMapper<TileRenderRequested, OutputMessage> mockedOutputMapper = mock();
        when(mockedOutputMapper.transform(any(TileRenderRequested.class))).thenThrow(exception);

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(eventStore, inputMapper, bufferOutputMapper, mockedOutputMapper, bufferEmitter, renderEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedOutputMapper).transform(any(TileRenderRequested.class));
        verifyNoMoreInteractions(mockedOutputMapper);

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenBufferEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TileRenderCompletedController(eventStore, inputMapper, bufferOutputMapper, renderOutputMapper, mockedEmitter, renderEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).send(any(OutputMessage.class));
        verifyNoMoreInteractions(mockedEmitter);

        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenRenderEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter mockedEmitter = mock();
        when(mockedEmitter.getTopicName()).thenReturn("render");
        when(mockedEmitter.send(any(OutputMessage.class), anyString())).thenReturn(Single.error(exception));

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(eventStore, inputMapper, bufferOutputMapper, renderOutputMapper, bufferEmitter, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).getTopicName();
        verify(mockedEmitter).send(any(OutputMessage.class), eq("render-requested-1"));
        verifyNoMoreInteractions(mockedEmitter);

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
    }

    @Test
    void shouldNotRequestRenderingWhenNumberOfLevelsIsEqualsToDraftLevels() {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 3);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(eventStore, inputMapper, bufferOutputMapper, renderOutputMapper, bufferEmitter, renderEmitter);

        controller.onNext(aInputMessage(4, 0, 0)).toCompletable().doOnError(Assertions::fail).await();

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldNotRequestRenderingWhenReceivingALateEvent() {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 3);
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(eventStore, inputMapper, bufferOutputMapper, renderOutputMapper, bufferEmitter, renderEmitter);

        controller.onNext(aLateEventInputMessage(4, 0, 0)).toCompletable().doOnError(Assertions::fail).await();

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    private static Stream<Arguments> notTerminalMessages() {
        return Stream.of(
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 0)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 0)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 1)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 1)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 1)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 2)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 2)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 2)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 3)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 1, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 1, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 1, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 2, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 2, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 2, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 3, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 3, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 3, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 0, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 0)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 0)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 1)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 1)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 1)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 2)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 2)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 2)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 3)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 5, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 5, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 5, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 6, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 6, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 6, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 7, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 7, 3)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 7, 3)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 8, 0)),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 7, 7)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 7, 7)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 0, 0)),
                        "render-requested-2"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 31, 31)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 31, 31)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 48, 48)),
                        "render-requested-2"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 63, 63)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 63, 63)),
                        TileRenderRequestedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 96, 96)),
                        "render-requested-3"
                )
        );
    }

    private static Stream<Arguments> terminalMessages() {
        return Stream.of(
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 31)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 63)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 95)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 127)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 127)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 31)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 63)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 95)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 127)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 127)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 31)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 63)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 95)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 127)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 127)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 31)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 63)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 95)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8),
                        TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 127)),
                        TileRenderCompletedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 127)),
                        "render-requested-4"
                )
        );
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, String revision, int levels) {
        return Design.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(USER_ID_1)
                .withData(DATA_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(revision)
                .withStatus("CREATED")
                .withLevels(levels)
                .withBitmap(TilesBitmap.empty().getBitmap())
                .withPublished(levels == LEVELS_READY)
                .withCreated(dateTime.minusHours(2))
                .withUpdated(dateTime.minusHours(1))
                .build();
    }

    @NotNull
    private static TileRenderCompleted aTileRenderCompleted(UUID designId, UUID commandId, String revision, int level, int row, int col) {
        return TileRenderCompleted.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(revision)
                .withStatus("CREATED")
                .withLevel(level)
                .withRow(row)
                .withCol(col)
                .build();
    }

    @NotNull
    private static TileRenderRequested aTileRenderRequested(UUID designId, UUID commandId, String revision, int level, int row, int col) {
        return TileRenderRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withData(DATA_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(revision)
                .withLevel(level)
                .withRow(row)
                .withCol(col)
                .build();
    }

    @NotNull
    private static InputMessage aInputMessage(int level, int row, int col) {
        return TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, level, row, col));
    }

    @NotNull
    private static InputMessage aLateEventInputMessage(int level, int row, int col) {
        return TileRenderCompletedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_2, REVISION_0, level, row, col));
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