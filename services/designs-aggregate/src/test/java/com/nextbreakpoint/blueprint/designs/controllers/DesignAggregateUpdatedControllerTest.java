package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentStatus;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.designs.TestConstants;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import org.apache.avro.specific.SpecificRecord;
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
import java.util.UUID;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DesignAggregateUpdatedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private static final Bitmap bitmap0 = Bitmap.empty();
    private static final Bitmap bitmap1 = Bitmap.empty().putTile(0, 0, 0);
    private static final Bitmap bitmap2 = Bitmap.empty().putTile(0, 0, 0).putTile(1, 1, 0);

    private final MessageEmitter<SpecificRecord> emitter = mock();

    private final DesignAggregateUpdatedController controller = new DesignAggregateUpdatedController(TestConstants.MESSAGE_SOURCE, emitter);

    @ParameterizedTest
    @MethodSource("someMessages")
    void shouldPublishAMessageToInformThatADocumentHasChanged(InputMessage<DesignAggregateUpdated> inputMessage, OutputMessage<Object> expectedOutputMessage) {
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
    void shouldReturnErrorWhenEmitterFails() {
        final RuntimeException exception = new RuntimeException();
        final MessageEmitter<SpecificRecord> mockedEmitter = mock();
        when(mockedEmitter.send(any(OutputMessage.class))).thenReturn(Single.error(exception));

        final var controller = new DesignAggregateUpdatedController(TestConstants.MESSAGE_SOURCE, mockedEmitter);

        assertThatThrownBy(() -> controller.onNext(anUpdateInputMessage()).toCompletable().await()).isEqualTo(exception);

        verify(mockedEmitter).send(any(OutputMessage.class));
        verifyNoMoreInteractions(emitter);
    }

    private static Stream<Arguments> someMessages() {
        return Stream.of(
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1))),
                        DesignDocumentUpdateRequestedFactory.createInputMessage(aMessageId(), aDesignDocumentUpdateRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1)))
                ),
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(aMessageId(), REVISION_1, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", true, LEVELS_READY, bitmap1, dateTime.minusHours(2), dateTime.minusHours(1))),
                        DesignDocumentUpdateRequestedFactory.createInputMessage(aMessageId(), aDesignDocumentUpdateRequested(DESIGN_ID_2, COMMAND_ID_2, USER_ID_2, DATA_2, REVISION_1, "UPDATED", true, LEVELS_READY, bitmap1, dateTime.minusHours(2), dateTime.minusHours(1)))
                ),
                Arguments.of(
                        DesignAggregateUpdatedFactory.of(aMessageId(), REVISION_2, dateTime.minusMinutes(1), aDesignAggregateUpdated(DESIGN_ID_3, COMMAND_ID_3, USER_ID_1, DATA_3, REVISION_2, "DELETED", false, LEVELS_DRAFT, bitmap2, dateTime.minusHours(2), dateTime.minusHours(1))),
                        DesignDocumentDeleteRequestedFactory.createOutputMessage(aMessageId(), aDesignDocumentDeleteRequested(DESIGN_ID_3, COMMAND_ID_3, REVISION_2))
                )
        );
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static DesignAggregateUpdated aDesignAggregateUpdated(UUID designId, UUID commandId, UUID userId, String data, String revision, String status, boolean published, int levels, Bitmap bitmap, LocalDateTime created, LocalDateTime updated) {
        return DesignAggregateUpdated.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setChecksum(Checksum.of(data))
                .setRevision(revision)
                .setStatus(DesignAggregateStatus.valueOf(status))
                .setData(data)
                .setPublished(published)
                .setLevels(levels)
                .setBitmap(bitmap.toByteBuffer())
                .setCreated(created.toInstant(ZoneOffset.UTC))
                .setUpdated(updated.toInstant(ZoneOffset.UTC))
                .build();
    }

    @NotNull
    private static DesignDocumentUpdateRequested aDesignDocumentUpdateRequested(UUID designId, UUID commandId, UUID userId, String data, String revision, String status, boolean published, int levels, Bitmap bitmap, LocalDateTime created, LocalDateTime updated) {
        return DesignDocumentUpdateRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setUserId(userId)
                .setChecksum(Checksum.of(data))
                .setRevision(revision)
                .setStatus(DesignDocumentStatus.valueOf(status))
                .setData(data)
                .setPublished(published)
                .setLevels(levels)
                .setTiles(bitmap.toTiles())
                .setCreated(created.toInstant(ZoneOffset.UTC))
                .setUpdated(updated.toInstant(ZoneOffset.UTC))
                .build();
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
    private InputMessage<DesignAggregateUpdated> anUpdateInputMessage() {
        return DesignAggregateUpdatedFactory.of(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "CREATED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1)));
    }

    @NotNull
    private InputMessage<DesignAggregateUpdated> aDeleteInputMessage() {
        return DesignAggregateUpdatedFactory.of(aMessageId(), REVISION_0, dateTime.minusMinutes(3), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_0, "DELETED", false, LEVELS_DRAFT, bitmap0, dateTime.minusHours(2), dateTime.minusHours(1)));
    }

    private static class DesignAggregateUpdatedFactory {
        @NotNull
        public static InputMessage<DesignAggregateUpdated> of(UUID messageId, String messageToken, LocalDateTime messageTime, DesignAggregateUpdated designAggregateUpdated) {
            return TestUtils.createInputMessage(
                    designAggregateUpdated.getDesignId().toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated, messageToken, messageTime
            );
        }
    }

    private static class DesignDocumentUpdateRequestedFactory {
        @NotNull
        public static OutputMessage<DesignDocumentUpdateRequested> createInputMessage(UUID messageId, DesignDocumentUpdateRequested designDocumentUpdateRequested) {
            return TestUtils.createOutputMessage(
                    designDocumentUpdateRequested.getDesignId().toString(), DESIGN_DOCUMENT_UPDATE_REQUESTED, messageId, designDocumentUpdateRequested
            );
        }
    }

    private static class DesignDocumentDeleteRequestedFactory {
        @NotNull
        public static OutputMessage<DesignDocumentDeleteRequested> createOutputMessage(UUID messageId, DesignDocumentDeleteRequested designDocumentDeleteRequested) {
            return TestUtils.createOutputMessage(
                    designDocumentDeleteRequested.getDesignId().toString(), DESIGN_DOCUMENT_DELETE_REQUESTED, messageId, designDocumentDeleteRequested
            );
        }
    }
}