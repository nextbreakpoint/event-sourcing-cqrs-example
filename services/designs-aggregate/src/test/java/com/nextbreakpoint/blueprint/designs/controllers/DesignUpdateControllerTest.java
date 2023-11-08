package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.MessageMapper;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdatedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedInputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import com.nextbreakpoint.blueprint.designs.TestUtils;
import com.nextbreakpoint.blueprint.designs.aggregate.DesignEventStore;
import com.nextbreakpoint.blueprint.designs.model.Design;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_AGGREGATE_UPDATED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DesignUpdateControllerTest {
    private static final Mapper<InputMessage, DesignInsertRequested> insertInputMapper = new DesignInsertRequestedInputMapper();
    private static final Mapper<InputMessage, DesignUpdateRequested> updateInputMapper = new DesignUpdateRequestedInputMapper();
    private static final Mapper<InputMessage, DesignDeleteRequested> deleteInputMapper = new DesignDeleteRequestedInputMapper();
    private static final MessageMapper<DesignAggregateUpdated, OutputMessage> updateOutputMapper = new DesignAggregateUpdatedOutputMapper(MESSAGE_SOURCE);
    private static final MessageMapper<TileRenderRequested, OutputMessage> renderOutputMapper = new TileRenderRequestedOutputMapper(MESSAGE_SOURCE);

    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final MessageEmitter updateEmitter = mock();
    private final MessageEmitter renderEmitter = mock();
    private final DesignEventStore eventStore = mock();

    private final DesignUpdateController.DesignInsertRequestedController insertController = new DesignUpdateController.DesignInsertRequestedController(eventStore, insertInputMapper, updateOutputMapper, renderOutputMapper, updateEmitter, renderEmitter);
    private final DesignUpdateController.DesignUpdateRequestedController updateController = new DesignUpdateController.DesignUpdateRequestedController(eventStore, updateInputMapper, updateOutputMapper, renderOutputMapper, updateEmitter, renderEmitter);
    private final DesignUpdateController.DesignDeleteRequestedController deleteController = new DesignUpdateController.DesignDeleteRequestedController(eventStore, deleteInputMapper, updateOutputMapper, renderOutputMapper, updateEmitter, renderEmitter);

    @Nested
    class InsertController {
        @Test
        void shouldPublishMessagesToInformThatTheDesignAggregateHasChangedAndRequestTheRenderingOf21Tiles() {
            final var design = aDesign(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1, REVISION_1, "CREATED", LEVELS_DRAFT, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusMinutes(3));
            final var inputMessage = DesignInsertRequestedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_1, dateTime.minusMinutes(3), aDesignInsertRequested(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, DATA_1));
            final var expectedOutputMessage = DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_1, USER_ID_1, REVISION_1, DATA_1, LEVELS_DRAFT, TilesBitmap.empty(), false, "CREATED", dateTime.minusHours(2), dateTime.minusMinutes(3)));

            when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
            when(eventStore.projectDesign(design.getDesignId(), inputMessage.getToken())).thenReturn(Single.just(Optional.of(design)));
            when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));
            when(updateEmitter.send(any())).thenReturn(Single.just(null));
            when(renderEmitter.getTopicName()).thenReturn("render");
            when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

            insertController.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(eventStore).appendMessage(inputMessage);
            verify(eventStore).projectDesign(design.getDesignId(), inputMessage.getToken());
            verify(eventStore).updateDesign(design);
            verifyNoMoreInteractions(eventStore);

            verify(updateEmitter).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
                assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
                assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
            }));
            verifyNoMoreInteractions(updateEmitter);

            final var expectedEvents = getExpectedTileRenderRequestEvents(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1);
            final Set<String> expectedData = expectedEvents.stream().map(Json::encodeValue).collect(Collectors.toSet());

            verify(renderEmitter, times(21)).getTopicName();
            verify(renderEmitter, times(21)).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(DESIGN_ID_1.toString());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(TILE_RENDER_REQUESTED);
                assertThat(message.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
                assertThat(expectedData.contains(message.getValue().getData())).isTrue();
            }), eq("render-requested-0"));
            verifyNoMoreInteractions(renderEmitter);
        }
    }

    @Nested
    class UpdateController {
        @Test
        void shouldPublishMessagesToInformThatTheDesignAggregateHasChangedAndRequestTheRenderingOf21Tiles() {
            final var design = aDesign(DESIGN_ID_1, COMMAND_ID_2, USER_ID_1, DATA_2, REVISION_2, "UPDATED", LEVELS_DRAFT, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusMinutes(3));
            final var inputMessage = DesignUpdateRequestedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_2, dateTime.minusMinutes(3), aDesignUpdateRequested(DESIGN_ID_1, COMMAND_ID_2, USER_ID_1, DATA_2, false));
            final var expectedOutputMessage = DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_2, USER_ID_1, REVISION_2, DATA_2, LEVELS_DRAFT, TilesBitmap.empty(), false, "UPDATED", dateTime.minusHours(2), dateTime.minusMinutes(3)));

            when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
            when(eventStore.projectDesign(design.getDesignId(), inputMessage.getToken())).thenReturn(Single.just(Optional.of(design)));
            when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));
            when(updateEmitter.send(any())).thenReturn(Single.just(null));
            when(renderEmitter.getTopicName()).thenReturn("render");
            when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

            updateController.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(eventStore).appendMessage(inputMessage);
            verify(eventStore).projectDesign(design.getDesignId(), inputMessage.getToken());
            verify(eventStore).updateDesign(design);
            verifyNoMoreInteractions(eventStore);

            verify(updateEmitter).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
                assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
                assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
            }));
            verifyNoMoreInteractions(updateEmitter);

            final var expectedEvents = getExpectedTileRenderRequestEvents(DESIGN_ID_1, COMMAND_ID_2, REVISION_2, DATA_2);
            final Set<String> expectedData = expectedEvents.stream().map(Json::encodeValue).collect(Collectors.toSet());

            verify(renderEmitter, times(21)).getTopicName();
            verify(renderEmitter, times(21)).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(DESIGN_ID_1.toString());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(TILE_RENDER_REQUESTED);
                assertThat(message.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
                assertThat(expectedData.contains(message.getValue().getData())).isTrue();
            }), eq("render-requested-0"));
            verifyNoMoreInteractions(renderEmitter);
        }

        @Test
        void shouldPublishMessagesToInformThatTheDesignAggregateHasChangedAndRequestTheRenderingOf21TilesRegardlessOfTheNumberOfLevels() {
            final var design = aDesign(DESIGN_ID_1, COMMAND_ID_2, USER_ID_1, DATA_2, REVISION_2, "UPDATED", LEVELS_READY, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusMinutes(3));
            final var inputMessage = DesignUpdateRequestedFactory.createInputMessage(DESIGN_ID_1, aMessageId(), REVISION_2, dateTime.minusMinutes(3), aDesignUpdateRequested(DESIGN_ID_1, COMMAND_ID_2, USER_ID_1, DATA_2, true));
            final var expectedOutputMessage = DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_1, aMessageId(), aDesignAggregateUpdated(DESIGN_ID_1, COMMAND_ID_2, USER_ID_1, REVISION_2, DATA_2, LEVELS_READY, TilesBitmap.empty(), true, "UPDATED", dateTime.minusHours(2), dateTime.minusMinutes(3)));

            when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
            when(eventStore.projectDesign(design.getDesignId(), inputMessage.getToken())).thenReturn(Single.just(Optional.of(design)));
            when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));
            when(updateEmitter.send(any())).thenReturn(Single.just(null));
            when(renderEmitter.getTopicName()).thenReturn("render");
            when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

            updateController.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(eventStore).appendMessage(inputMessage);
            verify(eventStore).projectDesign(design.getDesignId(), inputMessage.getToken());
            verify(eventStore).updateDesign(design);
            verifyNoMoreInteractions(eventStore);

            verify(updateEmitter).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
                assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
                assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
            }));
            verifyNoMoreInteractions(updateEmitter);

            final var expectedEvents = getExpectedTileRenderRequestEvents(DESIGN_ID_1, COMMAND_ID_2, REVISION_2, DATA_2);
            final Set<String> expectedData = expectedEvents.stream().map(Json::encodeValue).collect(Collectors.toSet());

            verify(renderEmitter, times(21)).getTopicName();
            verify(renderEmitter, times(21)).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(DESIGN_ID_1.toString());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(TILE_RENDER_REQUESTED);
                assertThat(message.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
                assertThat(expectedData.contains(message.getValue().getData())).isTrue();
            }), eq("render-requested-0"));
            verifyNoMoreInteractions(renderEmitter);
        }
    }

    @Nested
    class DeleteController {
        @Test
        void shouldPublishMessagesToInformThatTheDesignAggregateHasChanged() {
            final var design = aDesign(DESIGN_ID_2, COMMAND_ID_3, USER_ID_2, DATA_2, REVISION_2, "DELETED", LEVELS_DRAFT, TilesBitmap.empty(), dateTime.minusHours(2), dateTime.minusMinutes(3));
            final var inputMessage = DesignDeleteRequestedFactory.createInputMessage(DESIGN_ID_2, aMessageId(), REVISION_2, dateTime.minusMinutes(1), aDesignDeleteRequested(DESIGN_ID_2, COMMAND_ID_3, USER_ID_2));
            final var expectedOutputMessage = DesignAggregateUpdatedFactory.createOutputMessage(DESIGN_ID_2, aMessageId(), aDesignAggregateUpdated(DESIGN_ID_2, COMMAND_ID_3, USER_ID_2, REVISION_2, DATA_2, LEVELS_DRAFT, TilesBitmap.empty(), false, "DELETED", dateTime.minusHours(2), dateTime.minusMinutes(3)));

            when(eventStore.appendMessage(inputMessage)).thenReturn(Single.just(null));
            when(eventStore.projectDesign(design.getDesignId(), inputMessage.getToken())).thenReturn(Single.just(Optional.of(design)));
            when(eventStore.updateDesign(design)).thenReturn(Single.just(Optional.of(design)));
            when(updateEmitter.send(any())).thenReturn(Single.just(null));
            when(renderEmitter.getTopicName()).thenReturn("render");
            when(renderEmitter.send(any(), any())).thenReturn(Single.just(null));

            deleteController.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

            verify(eventStore).appendMessage(inputMessage);
            verify(eventStore).projectDesign(design.getDesignId(), inputMessage.getToken());
            verify(eventStore).updateDesign(design);
            verifyNoMoreInteractions(eventStore);

            verify(updateEmitter).send(assertArg(message -> {
                assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
                assertThat(message.getValue().getUuid()).isNotNull();
                assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
                assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
                assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
            }));
            verifyNoMoreInteractions(updateEmitter);

            verifyNoInteractions(renderEmitter);
        }
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
    private static DesignAggregateUpdated aDesignAggregateUpdated(UUID designId, UUID commandId, UUID userId, String revision, String data, int levels, TilesBitmap bitmap, boolean published, String status, LocalDateTime created, LocalDateTime updated) {
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
                .withCreated(created)
                .withUpdated(updated)
                .build();
    }

    @NotNull
    private static DesignInsertRequested aDesignInsertRequested(UUID designId, UUID commandId, UUID userId, String data) {
        return DesignInsertRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withData(data)
                .build();
    }

    @NotNull
    private static DesignUpdateRequested aDesignUpdateRequested(UUID designId, UUID commandId, UUID userId, String data, boolean published) {
        return DesignUpdateRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .withData(data)
                .withPublished(published)
                .build();
    }

    @NotNull
    private static DesignDeleteRequested aDesignDeleteRequested(UUID designId, UUID commandId, UUID userId) {
        return DesignDeleteRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withUserId(userId)
                .build();
    }

    @NotNull
    private static List<TileRenderRequested> getExpectedTileRenderRequestEvents(UUID designId, UUID commandId, String revision, String data) {
        return List.of(
                aTileRenderRequested(designId, commandId, revision, data, 0, 0, 0),
                aTileRenderRequested(designId, commandId, revision, data, 1, 0, 0),
                aTileRenderRequested(designId, commandId, revision, data, 1, 0, 1),
                aTileRenderRequested(designId, commandId, revision, data, 1, 1, 0),
                aTileRenderRequested(designId, commandId, revision, data, 1, 1, 1),
                aTileRenderRequested(designId, commandId, revision, data, 2, 0, 0),
                aTileRenderRequested(designId, commandId, revision, data, 2, 0, 1),
                aTileRenderRequested(designId, commandId, revision, data, 2, 0, 2),
                aTileRenderRequested(designId, commandId, revision, data, 2, 0, 3),
                aTileRenderRequested(designId, commandId, revision, data, 2, 1, 0),
                aTileRenderRequested(designId, commandId, revision, data, 2, 1, 1),
                aTileRenderRequested(designId, commandId, revision, data, 2, 1, 2),
                aTileRenderRequested(designId, commandId, revision, data, 2, 1, 3),
                aTileRenderRequested(designId, commandId, revision, data, 2, 2, 0),
                aTileRenderRequested(designId, commandId, revision, data, 2, 2, 1),
                aTileRenderRequested(designId, commandId, revision, data, 2, 2, 2),
                aTileRenderRequested(designId, commandId, revision, data, 2, 2, 3),
                aTileRenderRequested(designId, commandId, revision, data, 2, 3, 0),
                aTileRenderRequested(designId, commandId, revision, data, 2, 3, 1),
                aTileRenderRequested(designId, commandId, revision, data, 2, 3, 2),
                aTileRenderRequested(designId, commandId, revision, data, 2, 3, 3)
        );
    }

    @NotNull
    private static TileRenderRequested aTileRenderRequested(UUID designId, UUID commandId, String revision, String data, int level, int row, int col) {
        return TileRenderRequested.builder()
                .withDesignId(designId)
                .withCommandId(commandId)
                .withData(data)
                .withChecksum(Checksum.of(data))
                .withRevision(revision)
                .withLevel(level)
                .withRow(row)
                .withCol(col)
                .build();
    }

    private static class DesignInsertRequestedFactory {
        @NotNull
        public static InputMessage createInputMessage(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, DesignInsertRequested designInsertRequested) {
            return TestUtils.createInputMessage(
                    designId.toString(), DESIGN_INSERT_REQUESTED, messageId, designInsertRequested, messageToken, messageTime
            );
        }
    }

    private static class DesignUpdateRequestedFactory {
        @NotNull
        public static InputMessage createInputMessage(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, DesignUpdateRequested designUpdateRequested) {
            return TestUtils.createInputMessage(
                    designId.toString(), DESIGN_UPDATE_REQUESTED, messageId, designUpdateRequested, messageToken, messageTime
            );
        }
    }

    private static class DesignDeleteRequestedFactory {
        @NotNull
        public static InputMessage createInputMessage(UUID designId, UUID messageId, String messageToken, LocalDateTime messageTime, DesignDeleteRequested designDeleteRequested) {
            return TestUtils.createInputMessage(
                    designId.toString(), DESIGN_DELETE_REQUESTED, messageId, designDeleteRequested, messageToken, messageTime
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