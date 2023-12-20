package com.nextbreakpoint.blueprint.designs.controllers;

import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.designs.TestFactory;
import com.nextbreakpoint.blueprint.designs.common.NotificationPublisher;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
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
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesignDocumentDeleteCompletedControllerTest {
    private static final LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS);

    private final NotificationPublisher publisher = mock();

    private final DesignDocumentDeleteCompletedController controller = new DesignDocumentDeleteCompletedController(publisher);

    @Test
    void shouldPublishNotification() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        when(publisher.publish(any())).thenReturn(Single.just(null));

        controller.onNext(inputMessage).toCompletable().doOnError(Assertions::fail).await();

        final DesignChangedNotification expectedNotification = DesignChangedNotification.builder()
                .withKey(DESIGN_ID_1.toString())
                .withRevision(REVISION_1)
                .build();

        verify(publisher).publish(expectedNotification);
    }

    @Test
    void shouldReturnErrorWhenCannotPublishNotification() {
        final var inputMessage = TestFactory.createInputMessage(aMessageId(), "001", dateTime, aDesignDocumentDeleteCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_1));

        final var exception = new RuntimeException();
        when(publisher.publish(any())).thenReturn(Single.error(exception));

        assertThatThrownBy(() -> controller.onNext(inputMessage).toCompletable().await()).isEqualTo(exception);

        verify(publisher).publish(any());
    }

    @NotNull
    private static UUID aMessageId() {
        return UUID.randomUUID();
    }

    @NotNull
    private static DesignDocumentDeleteCompleted aDesignDocumentDeleteCompleted(UUID designId, UUID commandId, String revision) {
        return DesignDocumentDeleteCompleted.newBuilder()
                .setDesignId(designId)
                .setCommandId(commandId)
                .setRevision(revision)
                .build();
    }
}