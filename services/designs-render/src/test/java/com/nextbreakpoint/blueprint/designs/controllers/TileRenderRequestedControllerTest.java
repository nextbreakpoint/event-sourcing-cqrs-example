package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.core.MessageEmitter;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.common.TileRenderer;
import io.vertx.rxjava.core.WorkerExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TileRenderRequestedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final S3Driver driver = mock();
    private final WorkerExecutor executor = mock();
    private final TileRenderer renderer = mock();
    private final MessageEmitter<TileRenderCompleted> emitter = mock();

    private final TileRenderRequestedController controller = new TileRenderRequestedController(MESSAGE_SOURCE, emitter, executor, driver, renderer);

    @Test
    void shouldRenderTileAndPublishEventToNotifyRenderIsCompleted() {
//        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteRequested(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));
//        final var expectedOutputMessage = TestFactory.createOutputMessage(aMessageId(), aDesignDocumentDeleteCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));
//
//        when(store.deleteDesign(any(DeleteDesignRequest.class))).thenReturn(Single.just(null));
//        when(emitter.send(any())).thenReturn(Single.just(null));
//
//        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();
//
//        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(false).build());
//        verify(store).deleteDesign(DeleteDesignRequest.builder().withUuid(DESIGN_ID_1).withDraft(true).build());
//        verifyNoMoreInteractions(store);
//
//        verify(emitter).send(assertArg(message -> hasExpectedDesignDocumentDeleteCompleted(message, expectedOutputMessage)));
//        verifyNoMoreInteractions(emitter);
    }
}