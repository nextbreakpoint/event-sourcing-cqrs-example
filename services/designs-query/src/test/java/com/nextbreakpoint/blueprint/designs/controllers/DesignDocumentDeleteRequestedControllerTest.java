package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DesignDocumentDeleteRequestedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final Store store = mock();
    private final MessageEmitter<DesignDocumentDeleteCompleted> emitter = mock();

    private final DesignDocumentDeleteRequestedController controller = new DesignDocumentDeleteRequestedController(MESSAGE_SOURCE, store, emitter);

    @Test
    void shouldDeleteDraftAndNotDraftDocumentsAndPublishAnEvent() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentDeleteCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).build());
        verifyNoMoreInteractions(store);

        verify(emitter).send(assertArg(message -> hasExpectedDesignDocumentDeleteCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenStoreFailsWhileDeletingDraftDocument() {
        final RuntimeException exception = new RuntimeException();
        final Store mockedStore = mock();
        when(mockedStore.deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build())).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        final var controller = new DesignDocumentDeleteRequestedController(MESSAGE_SOURCE, mockedStore, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(mockedStore).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verifyNoMoreInteractions(mockedStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenStoreFailsWhileDeletingDocument() {
        final RuntimeException exception = new RuntimeException();
        final Store mockedStore = mock();
        when(mockedStore.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));
        when(mockedStore.deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).build())).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        final var controller = new DesignDocumentDeleteRequestedController(MESSAGE_SOURCE, mockedStore, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(mockedStore).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verify(mockedStore).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).build());
        verifyNoMoreInteractions(mockedStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter<DesignDocumentDeleteCompleted> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentDeleteCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));

        final var controller = new DesignDocumentDeleteRequestedController(MESSAGE_SOURCE, store, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).build());
        verifyNoMoreInteractions(store);

        verify(mockedEmitter).send(assertArg(message -> hasExpectedDesignDocumentDeleteCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(mockedEmitter);
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static DesignDocumentDeleteRequested aDesignDocumentDeleteRequested(UUID designId, UUID commandId, String revision) {
        return DesignDocumentDeleteRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .build();
    }

    @NotNull
    private static DesignDocumentDeleteCompleted aDesignDocumentDeleteCompleted(UUID designId, UUID commandId, String revision) {
        return DesignDocumentDeleteCompleted.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .build();
    }

    private static void hasExpectedDesignDocumentDeleteCompleted(OutputMessage<DesignDocumentDeleteCompleted> message, OutputMessage<DesignDocumentDeleteCompleted> expectedOutputMessage) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
        softly.assertThat(message.getValue().getUuid()).isNotNull();
        softly.assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
        softly.assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
        softly.assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        softly.assertAll();
    }
}