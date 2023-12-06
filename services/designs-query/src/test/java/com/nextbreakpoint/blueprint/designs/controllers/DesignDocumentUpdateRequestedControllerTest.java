package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignRequest;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DesignDocumentUpdateRequestedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final Store store = mock();
    private final MessageEmitter<DesignDocumentUpdateCompleted> emitter = mock();

    private final DesignDocumentUpdateRequestedController controller = new DesignDocumentUpdateRequestedController(MESSAGE_SOURCE, store, emitter);

    @Test
    void shouldInsertDocumentAndPublishAnEvent() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aPublishedDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, DATA_1);

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 100, true));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentUpdateCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.insertDesign(any(InsertDesignRequest.class))).thenReturn(Single.just(null));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).withDesign(design).build());
        verifyNoMoreInteractions(store);

        verify(emitter).send(assertArg(message -> hasExpectedDesignDocumentUpdateCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldDeleteDocumentAndPublishAnEventWhenNotPublished() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 100, false, DATA_1);

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 100, false));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentUpdateCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.insertDesign(any(InsertDesignRequest.class))).thenReturn(Single.just(null));
        when(store.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verifyNoMoreInteractions(store);

        verify(emitter).send(assertArg(message -> hasExpectedDesignDocumentUpdateCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldDeleteDocumentAndPublishAnEventWhenNotCompleted() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 50, true, DATA_1);

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 50, true));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentUpdateCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.insertDesign(any(InsertDesignRequest.class))).thenReturn(Single.just(null));
        when(store.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verifyNoMoreInteractions(store);

        verify(emitter).send(assertArg(message -> hasExpectedDesignDocumentUpdateCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldDeleteDocumentAndPublishAnEventWhenDraft() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 3, 100, true, DATA_1);

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 3, 100, true));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentUpdateCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.insertDesign(any(InsertDesignRequest.class))).thenReturn(Single.just(null));
        when(store.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));
        when(emitter.send(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verifyNoMoreInteractions(store);

        verify(emitter).send(assertArg(message -> hasExpectedDesignDocumentUpdateCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenStoreFailsWhileInsertingDraftDocument() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aPublishedDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, DATA_1);

        final var exception = new RuntimeException();
        final Store mockedStore = mock();
        when(mockedStore.insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build())).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 100, true));

        final var controller = new DesignDocumentUpdateRequestedController(MESSAGE_SOURCE, mockedStore, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(mockedStore).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verifyNoMoreInteractions(mockedStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenStoreFailsWhileInsertingDocument() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aPublishedDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, DATA_1);

        final var exception = new RuntimeException();
        final Store mockedStore = mock();
        when(mockedStore.insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build())).thenReturn(Single.just(null));
        when(mockedStore.insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).withDesign(design).build())).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 100, true));

        final var controller = new DesignDocumentUpdateRequestedController(MESSAGE_SOURCE, mockedStore, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(mockedStore).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(mockedStore).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).withDesign(design).build());
        verifyNoMoreInteractions(mockedStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenStoreFailsWhileDeletingDocument() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aDraftDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, DATA_1);

        final var exception = new RuntimeException();
        final Store mockedStore = mock();
        when(mockedStore.insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build())).thenReturn(Single.just(null));
        when(mockedStore.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 3, 100, false));

        final var controller = new DesignDocumentUpdateRequestedController(MESSAGE_SOURCE, mockedStore, emitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(mockedStore).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(mockedStore).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
        verifyNoMoreInteractions(mockedStore);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final var updated = Instant.now(Clock.systemUTC());
        final var created = updated.minus(1, DAYS);

        final var design = TestUtils.aPublishedDesign(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, DATA_1);

        final var exception = new RuntimeException();
        final MessageEmitter<DesignDocumentUpdateCompleted> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, created, updated, 8, 100, true));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentUpdateCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(store.insertDesign(any(InsertDesignRequest.class))).thenReturn(Single.just(null));

        final var controller = new DesignDocumentUpdateRequestedController(MESSAGE_SOURCE, store, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).withDesign(design).build());
        verify(store).insertDesign(InsertDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).withDesign(design).build());
        verifyNoMoreInteractions(store);

        verify(mockedEmitter).send(assertArg(message -> hasExpectedDesignDocumentUpdateCompleted(message, expectedOutputMessage)));
        verifyNoMoreInteractions(mockedEmitter);
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static DesignDocumentUpdateRequested aDesignDocumentUpdateRequested(UUID designId, UUID commandId, String revision, Instant created, Instant updated, int levels, int percentage, boolean published) {
        return DesignDocumentUpdateRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(USER_ID_1)
                .setData(DATA_1)
                .setChecksum(Checksum.of(DATA_1))
                .setRevision(revision)
                .setLevels(levels)
                .setTiles(TestUtils.getTiles(levels, percentage))
                .setStatus(DesignDocumentStatus.CREATED)
                .setPublished(published)
                .setCreated(created)
                .setUpdated(updated)
                .build();
    }

    @NotNull
    private static DesignDocumentUpdateCompleted aDesignDocumentUpdateCompleted(UUID designId, UUID commandId, String revision) {
        return DesignDocumentUpdateCompleted.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .build();
    }

    private static void hasExpectedDesignDocumentUpdateCompleted(OutputMessage<DesignDocumentUpdateCompleted> message, OutputMessage<DesignDocumentUpdateCompleted> expectedOutputMessage) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
        softly.assertThat(message.getValue().getUuid()).isNotNull();
        softly.assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
        softly.assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
        softly.assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        softly.assertAll();
    }
}