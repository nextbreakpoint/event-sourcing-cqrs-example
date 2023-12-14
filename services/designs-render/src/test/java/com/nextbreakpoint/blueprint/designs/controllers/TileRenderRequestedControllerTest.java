package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TileStatus;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import com.nextbreakpoint.blueprint.designs.common.AsyncTileRenderer;
import com.nextbreakpoint.blueprint.designs.common.Bucket;
import com.nextbreakpoint.blueprint.designs.common.Result;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.common.events.avro.TileStatus.COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TileRenderRequestedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final S3Driver driver = mock();
    private final AsyncTileRenderer renderer = mock();
    private final MessageEmitter<TileRenderCompleted> emitter = mock();

    private final TileRenderRequestedController controller = new TileRenderRequestedController(MESSAGE_SOURCE, emitter, driver, renderer);

    @Test
    void shouldRenderTileAndPublishEventToNotifyRenderIsCompleted() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, 0, 0, 0));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, 0, 0, 0, COMPLETED));

        final byte[] image = { 0, 1, 2 };

        when(driver.getObject(any())).thenReturn(Single.error(null));
        when(renderer.renderImage(any())).thenReturn(Single.just(Result.of(image, null)));
        when(driver.putObject(any(), any(byte[].class))).thenReturn(Single.just(null));
        when(emitter.send(any(), any())).thenReturn(Single.just(null));
        when(emitter.getTopicName()).thenReturn("render");

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(driver).getObject(Bucket.createBucketKey(inputMessage.getValue().getData()));
        verify(driver).putObject(Bucket.createBucketKey(inputMessage.getValue().getData()), image);
        verifyNoMoreInteractions(driver);

        verify(renderer).renderImage(inputMessage.getValue().getData());
        verifyNoMoreInteractions(renderer);

        verify(emitter).getTopicName();
        verify(emitter).send(assertArg(message -> hasExpectedTileRenderCompleted(message, expectedOutputMessage)), eq("render-completed-0"));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnCachedImageAndPublishEventToNotifyRenderIsCompleted() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aTileRenderRequested(DESIGN_ID_2, COMMAND_ID_2, REVISION_2, DATA_2, 0, 0, 0));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_2, REVISION_2, DATA_2, 0, 0, 0, COMPLETED));

        final byte[] image = { 0, 1, 2 };

        when(driver.getObject(any())).thenReturn(Single.just(image));
        when(emitter.send(any(), any())).thenReturn(Single.just(null));
        when(emitter.getTopicName()).thenReturn("render");

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        verify(driver).getObject(Bucket.createBucketKey(inputMessage.getValue().getData()));
        verifyNoMoreInteractions(driver);

        verifyNoInteractions(renderer);

        verify(emitter).getTopicName();
        verify(emitter).send(assertArg(message -> hasExpectedTileRenderCompleted(message, expectedOutputMessage)), eq("render-completed-0"));
        verifyNoMoreInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenDriverFails() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, 0, 0, 0));

        final byte[] image = { 0, 1, 2 };
        final var exception = new RuntimeException();

        when(driver.getObject(any())).thenReturn(Single.error(null));
        when(renderer.renderImage(any())).thenReturn(Single.just(Result.of(image, null)));
        when(driver.putObject(any(), any(byte[].class))).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(driver).getObject(Bucket.createBucketKey(inputMessage.getValue().getData()));
        verify(driver).putObject(Bucket.createBucketKey(inputMessage.getValue().getData()), image);
        verifyNoMoreInteractions(driver);

        verify(renderer).renderImage(inputMessage.getValue().getData());
        verifyNoMoreInteractions(renderer);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenRendererFails() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, 0, 0, 0));

        final var exception = new RuntimeException();

        when(driver.getObject(any())).thenReturn(Single.error(null));
        when(renderer.renderImage(any())).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(driver).getObject(Bucket.createBucketKey(inputMessage.getValue().getData()));
        verifyNoMoreInteractions(driver);

        verify(renderer).renderImage(inputMessage.getValue().getData());
        verifyNoMoreInteractions(renderer);

        verifyNoInteractions(emitter);
    }

    @Test
    void shouldReturnErrorWhenEmitterFails() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aTileRenderRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, 0, 0, 0));
        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1, DATA_1, 0, 0, 0, COMPLETED));

        final byte[] image = { 0, 1, 2 };
        final var exception = new RuntimeException();

        when(driver.getObject(any())).thenReturn(Single.error(null));
        when(renderer.renderImage(any())).thenReturn(Single.just(Result.of(image, null)));
        when(driver.putObject(any(), any(byte[].class))).thenReturn(Single.just(null));
        when(emitter.send(any(), any())).thenReturn(Single.error(exception));
        when(emitter.getTopicName()).thenReturn("render");

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(driver).getObject(Bucket.createBucketKey(inputMessage.getValue().getData()));
        verify(driver).putObject(Bucket.createBucketKey(inputMessage.getValue().getData()), image);
        verifyNoMoreInteractions(driver);

        verify(renderer).renderImage(inputMessage.getValue().getData());
        verifyNoMoreInteractions(renderer);

        verify(emitter).getTopicName();
        verify(emitter).send(assertArg(message -> hasExpectedTileRenderCompleted(message, expectedOutputMessage)), eq("render-completed-0"));
        verifyNoMoreInteractions(emitter);
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static TileRenderRequested aTileRenderRequested(UUID designId, UUID commandId, String revision, String data, int level, int col, int row) {
        return TileRenderRequested.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .setData(data)
                .setChecksum(Checksum.of(data))
                .setLevel(level)
                .setRow(row)
                .setCol(col)
                .build();
    }

    @NotNull
    private static TileRenderCompleted aTileRenderCompleted(UUID designId, UUID commandId, String revision, String data, int level, int col, int row, TileStatus status) {
        return TileRenderCompleted.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .setChecksum(Checksum.of(data))
                .setLevel(level)
                .setRow(row)
                .setCol(col)
                .setStatus(status)
                .build();
    }

    private static void hasExpectedTileRenderCompleted(OutputMessage<TileRenderCompleted> message, OutputMessage<TileRenderCompleted> expectedOutputMessage) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(message.getKey()).isEqualTo(expectedOutputMessage.getKey());
        softly.assertThat(message.getValue().getUuid()).isNotNull();
        softly.assertThat(message.getValue().getType()).isEqualTo(expectedOutputMessage.getValue().getType());
        softly.assertThat(message.getValue().getData()).isEqualTo(expectedOutputMessage.getValue().getData());
        softly.assertThat(message.getValue().getSource()).isEqualTo(expectedOutputMessage.getValue().getSource());
        softly.assertAll();
    }
}