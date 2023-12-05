package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.commands.avro.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.commands.avro.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DesignCommandControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final Store store = mock();
    private final MessageEmitter<DesignInsertRequested> insertEmitter = mock();
    private final MessageEmitter<DesignUpdateRequested> updateEmitter = mock();
    private final MessageEmitter<DesignDeleteRequested> deleteEmitter = mock();

    private final DesignCommandController.DesignInsertCommandController insertController = new DesignCommandController.DesignInsertCommandController(MESSAGE_SOURCE, store, insertEmitter);
    private final DesignCommandController.DesignUpdateCommandController updateController = new DesignCommandController.DesignUpdateCommandController(MESSAGE_SOURCE, store, updateEmitter);
    private final DesignCommandController.DesignDeleteCommandController deleteController = new DesignCommandController.DesignDeleteCommandController(MESSAGE_SOURCE, store, deleteEmitter);

    @Nested
    class InsertController {
        @Test
        void shouldSaveTheCommandAndPublishAnEvent() {
            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignInsertCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));
            final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignInsertRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));

            when(store.appendMessage(any())).thenReturn(Single.just(null));
            when(insertEmitter.send(any())).thenReturn(Single.just(null));

            insertController.onNext(commandMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(store).appendMessage(commandMessage);
            verifyNoMoreInteractions(store);

            verify(insertEmitter).send(assertArg(message -> hasExpectedDesignInsertRequested(message, expectedOutputMessage)));
            verifyNoMoreInteractions(insertEmitter);
        }

        @Test
        void shouldReturnErrorWhenStoreFails() {
            final RuntimeException exception = new RuntimeException();
            final Store mockedStore = mock();
            when(mockedStore.appendMessage(any(InputMessage.class))).thenReturn(Single.error(exception));

            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignInsertCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));

            final var insertController = new DesignCommandController.DesignInsertCommandController(MESSAGE_SOURCE, mockedStore, insertEmitter);

            assertThatThrownBy(() -> insertController.onNext(commandMessage).toCompletable().await()).isEqualTo(exception);

            verify(mockedStore).appendMessage(commandMessage);
            verifyNoMoreInteractions(mockedStore);

            verifyNoInteractions(insertEmitter);
        }

        @Test
        void shouldReturnErrorWhenEmitterFails() {
            final RuntimeException exception = new RuntimeException();
            final MessageEmitter<DesignInsertRequested> mockedEmitter = mock();
            when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignInsertCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));
            final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignInsertRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));

            when(store.appendMessage(any())).thenReturn(Single.just(null));

            final var insertController = new DesignCommandController.DesignInsertCommandController(MESSAGE_SOURCE, store, mockedEmitter);

            assertThatThrownBy(() -> insertController.onNext(commandMessage).toCompletable().await()).isEqualTo(exception);

            verify(store).appendMessage(commandMessage);
            verifyNoMoreInteractions(store);

            verify(mockedEmitter).send(assertArg(message -> hasExpectedDesignInsertRequested(message, expectedOutputMessage)));
            verifyNoMoreInteractions(mockedEmitter);
        }
    }

    @Nested
    class UpdateController {
        @Test
        void shouldSaveTheCommandAndPublishAnEvent() {
            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignUpdateCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_2, true));
            final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_2, true));

            when(store.appendMessage(any())).thenReturn(Single.just(null));
            when(updateEmitter.send(any())).thenReturn(Single.just(null));

            updateController.onNext(commandMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(store).appendMessage(commandMessage);
            verifyNoMoreInteractions(store);

            verify(updateEmitter).send(assertArg(message -> hasExpectedDesignUpdateRequested(message, expectedOutputMessage)));
            verifyNoMoreInteractions(updateEmitter);
        }

        @Test
        void shouldReturnErrorWhenStoreFails() {
            final RuntimeException exception = new RuntimeException();
            final Store mockedStore = mock();
            when(mockedStore.appendMessage(any(InputMessage.class))).thenReturn(Single.error(exception));

            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignUpdateCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, true));

            final var updateController = new DesignCommandController.DesignUpdateCommandController(MESSAGE_SOURCE, mockedStore, updateEmitter);

            assertThatThrownBy(() -> updateController.onNext(commandMessage).toCompletable().await()).isEqualTo(exception);

            verify(mockedStore).appendMessage(commandMessage);
            verifyNoMoreInteractions(mockedStore);

            verifyNoInteractions(insertEmitter);
        }

        @Test
        void shouldReturnErrorWhenEmitterFails() {
            final RuntimeException exception = new RuntimeException();
            final MessageEmitter<DesignUpdateRequested> mockedEmitter = mock();
            when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignUpdateCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, true));
            final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, true));

            when(store.appendMessage(any())).thenReturn(Single.just(null));

            final var updateController = new DesignCommandController.DesignUpdateCommandController(MESSAGE_SOURCE, store, mockedEmitter);

            assertThatThrownBy(() -> updateController.onNext(commandMessage).toCompletable().await()).isEqualTo(exception);

            verify(store).appendMessage(commandMessage);
            verifyNoMoreInteractions(store);

            verify(mockedEmitter).send(assertArg(message -> hasExpectedDesignUpdateRequested(message, expectedOutputMessage)));
            verifyNoMoreInteractions(mockedEmitter);
        }
    }

    @Nested
    class DeleteController {
        @Test
        void shouldSaveTheCommandAndPublishAnEvent() {
            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDeleteCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));
            final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));

            when(store.appendMessage(any())).thenReturn(Single.just(null));
            when(deleteEmitter.send(any())).thenReturn(Single.just(null));

            deleteController.onNext(commandMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(store).appendMessage(commandMessage);
            verifyNoMoreInteractions(store);

            verify(deleteEmitter).send(assertArg(message -> hasExpectedDesignDeleteRequested(message, expectedOutputMessage)));
            verifyNoMoreInteractions(deleteEmitter);
        }

        @Test
        void shouldReturnErrorWhenStoreFails() {
            final RuntimeException exception = new RuntimeException();
            final Store mockedStore = mock();
            when(mockedStore.appendMessage(any(InputMessage.class))).thenReturn(Single.error(exception));

            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDeleteCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));

            final var deleteController = new DesignCommandController.DesignDeleteCommandController(MESSAGE_SOURCE, mockedStore, deleteEmitter);

            assertThatThrownBy(() -> deleteController.onNext(commandMessage).toCompletable().await()).isEqualTo(exception);

            verify(mockedStore).appendMessage(commandMessage);
            verifyNoMoreInteractions(mockedStore);

            verifyNoInteractions(insertEmitter);
        }

        @Test
        void shouldReturnErrorWhenEmitterFails() {
            final RuntimeException exception = new RuntimeException();
            final MessageEmitter<DesignDeleteRequested> mockedEmitter = mock();
            when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

            final var commandMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDeleteCommand(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));
            final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1));

            when(store.appendMessage(any())).thenReturn(Single.just(null));

            final var deleteController = new DesignCommandController.DesignDeleteCommandController(MESSAGE_SOURCE, store, mockedEmitter);

            assertThatThrownBy(() -> deleteController.onNext(commandMessage).toCompletable().await()).isEqualTo(exception);

            verify(store).appendMessage(commandMessage);
            verifyNoMoreInteractions(store);

            verify(mockedEmitter).send(assertArg(message -> hasExpectedDesignDeleteRequested(message, expectedOutputMessage)));
            verifyNoMoreInteractions(mockedEmitter);
        }
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static DesignInsertCommand aDesignInsertCommand(UUID designId, UUID commandId, UUID userId, String data) {
        return DesignInsertCommand.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setData(data)
                .build();
    }

    @NotNull
    private static DesignUpdateCommand aDesignUpdateCommand(UUID designId, UUID commandId, UUID userId, String data, boolean published) {
        return DesignUpdateCommand.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setData(data)
                .setPublished(published)
                .build();
    }

    @NotNull
    private static DesignDeleteCommand aDesignDeleteCommand(UUID designId, UUID commandId, UUID userId) {
        return DesignDeleteCommand.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .build();
    }

    @NotNull
    private static DesignInsertRequested aDesignInsertRequested(UUID designId, UUID commandId, UUID userId, String data) {
        return DesignInsertRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setData(data)
                .build();
    }

    @NotNull
    private static DesignUpdateRequested aDesignUpdateRequested(UUID designId, UUID commandId, UUID userId, String data, boolean published) {
        return DesignUpdateRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setData(data)
                .setPublished(published)
                .build();
    }

    @NotNull
    private static DesignDeleteRequested aDesignDeleteRequested(UUID designId, UUID commandId, UUID userId) {
        return DesignDeleteRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .build();
    }

    private static void hasExpectedDesignInsertRequested(OutputMessage<DesignInsertRequested> message, OutputMessage<DesignInsertRequested> expectedOutputMessage) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
        softly.assertThat(message.getValue().getUuid()).isNotNull();
        softly.assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
        softly.assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
        softly.assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        softly.assertAll();
    }

    private static void hasExpectedDesignUpdateRequested(OutputMessage<DesignUpdateRequested> message, OutputMessage<DesignUpdateRequested> expectedOutputMessage) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
        softly.assertThat(message.getValue().getUuid()).isNotNull();
        softly.assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
        softly.assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
        softly.assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        softly.assertAll();
    }

    private static void hasExpectedDesignDeleteRequested(OutputMessage<DesignDeleteRequested> message, OutputMessage<DesignDeleteRequested> expectedOutputMessage) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
        softly.assertThat(message.getValue().getUuid()).isNotNull();
        softly.assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
        softly.assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
        softly.assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        softly.assertAll();
    }
}