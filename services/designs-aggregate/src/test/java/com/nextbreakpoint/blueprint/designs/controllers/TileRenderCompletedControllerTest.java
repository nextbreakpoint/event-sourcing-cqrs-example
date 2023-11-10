package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TileStatus;
import com.nextbreakpoint.blueprint.designs.TestConstants;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import com.nextbreakpoint.blueprint.designs.common.Render;
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
import java.util.function.Function;
import java.util.stream.Stream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
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
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter<TileRenderCompleted> bufferEmitter = mock();
    private final MessageEmitter<TileRenderRequested> renderEmitter = mock();
    private final DesignEventStore eventStore = mock();

    private final TileRenderCompletedController controller = new TileRenderCompletedController(TestConstants.MESSAGE_SOURCE, eventStore, bufferEmitter, renderEmitter);

    @ParameterizedTest
    @MethodSource("notTerminalMessages")
    void shouldPublishAMessageToInformThatATileHasBeenCompletedAndRequestTheRenderingOfTheNextTile(Design design, InputMessage<TileRenderCompleted> inputMessage, OutputMessage<TileRenderCompleted> expectedOutputMessage1, OutputMessage<TileRenderRequested> expectedOutputMessage2, String topicName) {
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
    void shouldPublishAMessageToInformThatATileHasBeenCompletedButNotRequestTheRenderingOfAnotherTile(Design design, InputMessage<TileRenderCompleted> inputMessage, OutputMessage<TileRenderCompleted> expectedOutputMessage) {
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
    void shouldReturnErrorWhenBufferEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter<TileRenderCompleted> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1));
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));

        final var controller = new TileRenderCompletedController(TestConstants.MESSAGE_SOURCE, eventStore, mockedEmitter, renderEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).send(any(OutputMessage.class));
        verifyNoMoreInteractions(mockedEmitter);

        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldReturnErrorWhenRenderEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter<TileRenderRequested> mockedEmitter = mock();
        when(mockedEmitter.getTopicName()).thenReturn("render");
        when(mockedEmitter.send(any(OutputMessage.class), anyString())).thenReturn(Single.error(exception));

        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1));
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(TestConstants.MESSAGE_SOURCE, eventStore, bufferEmitter, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(aInputMessage(4, 0, 0)).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).getTopicName();
        verify(mockedEmitter).send(any(OutputMessage.class), eq("render-requested-1"));
        verifyNoMoreInteractions(mockedEmitter);

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
    }

    @Test
    void shouldNotRequestRenderingWhenNumberOfLevelsIsEqualsToDraftLevels() {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 3, dateTime.minusHours(2), dateTime.minusHours(1));
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(TestConstants.MESSAGE_SOURCE, eventStore, bufferEmitter, renderEmitter);

        controller.onNext(aInputMessage(4, 0, 0)).toCompletable().doOnError(Assertions::fail).await();

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    @Test
    void shouldNotRequestRenderingWhenReceivingALateEvent() {
        final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 3, dateTime.minusHours(2), dateTime.minusHours(1));
        when(eventStore.findDesign(design.getDesignId())).thenReturn(Single.just(Optional.of(design)));
        when(bufferEmitter.send(any())).thenReturn(Single.just(null));

        final var controller = new TileRenderCompletedController(TestConstants.MESSAGE_SOURCE, eventStore, bufferEmitter, renderEmitter);

        controller.onNext(aLateEventInputMessage(4, 0, 0)).toCompletable().doOnError(Assertions::fail).await();

        verify(bufferEmitter).send(any());
        verifyNoMoreInteractions(bufferEmitter);
        verifyNoInteractions(renderEmitter);
    }

    private static Stream<Arguments> notTerminalMessages() {
        return Stream.of(
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 0)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 0)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 1), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 1)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 1)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 2), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 2)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 2)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 3), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 0, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 1, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 1, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 1, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 2, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 2, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 2, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 3, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 3, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 3, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 0, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 0)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 0)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 1), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 1)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 1)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 2), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 2)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 2)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 3), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 4, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 5, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 5, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 5, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 6, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 6, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 6, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 7, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 7, 3)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 4, 7, 3)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 8, 0), Render::createRenderKey),
                        "render-requested-1"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 7, 7)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 7, 7)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 0, 0), Render::createRenderKey),
                        "render-requested-2"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 31, 31)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 5, 31, 31)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 48, 48), Render::createRenderKey),
                        "render-requested-2"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 63, 63)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 6, 63, 63)),
                        TileRenderRequestedFactory.createOutputMessage(aMessageId(), aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 96, 96), Render::createRenderKey),
                        "render-requested-3"
                )
        );
    }

    private static Stream<Arguments> terminalMessages() {
        return Stream.of(
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 31)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 63)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 95)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 127)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 31, 127)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 31)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 63)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 95)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 127)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 63, 127)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 31)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 63)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 95)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 127)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 95, 127)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 31)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 31)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 63)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 63)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 95)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 95)),
                        "render-requested-4"
                ),
                Arguments.of(
                        aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 8, dateTime.minusHours(2), dateTime.minusHours(1)),
                        TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 127)),
                        TileRenderCompletedFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, 7, 127, 127)),
                        "render-requested-4"
                )
        );
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static Design aDesign(UUID designId, UUID commandId, String revision, int levels, LocalDateTime created, LocalDateTime updated) {
        return Design.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(USER_ID_1)
                .withData(DATA_1)
                .withChecksum(Checksum.of(DATA_1))
                .withRevision(revision)
                .withStatus("CREATED")
                .withLevels(levels)
                .withBitmap(Bitmap.empty().toByteBuffer())
                .withPublished(levels == LEVELS_READY)
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static TileRenderCompleted aTileRenderCompleted(UUID designId, UUID commandId, String revision, int level, int row, int col) {
        return TileRenderCompleted.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setChecksum(Checksum.of(DATA_1))
                .setRevision(revision)
                .setStatus(TileStatus.COMPLETED)
                .setLevel(level)
                .setRow(row)
                .setCol(col)
                .build();
    }

    @NotNull
    private static TileRenderRequested aTileRenderRequested(UUID designId, UUID commandId, String revision, int level, int row, int col) {
        return TileRenderRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setData(DATA_1)
                .setChecksum(Checksum.of(DATA_1))
                .setRevision(revision)
                .setLevel(level)
                .setRow(row)
                .setCol(col)
                .build();
    }

    @NotNull
    private static InputMessage<TileRenderCompleted> aInputMessage(int level, int row, int col) {
        return TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0, level, row, col));
    }

    @NotNull
    private static InputMessage<TileRenderCompleted> aLateEventInputMessage(int level, int row, int col) {
        return TileRenderCompletedFactory.createInputMessage(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_2, REVISION_0, level, row, col));
    }

    private static class TileRenderCompletedFactory {
        @NotNull
        public static InputMessage<TileRenderCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted) {
            return TestUtils.createInputMessage(
                    tileRenderCompleted.getDesignId().toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
            );
        }

        @NotNull
        public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted) {
            return TestUtils.createOutputMessage(
                    tileRenderCompleted.getDesignId().toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
            );
        }

        @NotNull
        public static InputMessage<TileRenderCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted, Function<TileRenderCompleted, String> keyMapper) {
            return TestUtils.createInputMessage(
                    keyMapper.apply(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
            );
        }

        @NotNull
        public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted, Function<TileRenderCompleted, String> keyMapper) {
            return TestUtils.createOutputMessage(
                    keyMapper.apply(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
            );
        }
    }

    private static class TileRenderRequestedFactory {
        @NotNull
        public static OutputMessage<TileRenderRequested> createOutputMessage(UUID messageId, TileRenderRequested tileRenderRequested) {
            return TestUtils.createOutputMessage(
                    tileRenderRequested.getDesignId().toString(), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
            );
        }

        @NotNull
        public static OutputMessage<TileRenderRequested> createOutputMessage(UUID messageId, TileRenderRequested tileRenderRequested, Function<TileRenderRequested, String> keyMapper) {
            return TestUtils.createOutputMessage(
                    keyMapper.apply(tileRenderRequested), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
            );
        }
    }
}