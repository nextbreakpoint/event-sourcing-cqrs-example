package com.nextbreakpoint.blueprint.designs.handlers;

import com.nextbreakpoint.blueprint.designs.common.EventBusAdapter;
import com.nextbreakpoint.blueprint.designs.common.MessageConsumerAdapter;
import com.nextbreakpoint.blueprint.designs.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.designs.model.DesignChangedNotification;
import com.nextbreakpoint.blueprint.designs.model.SessionUpdatedNotification;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class WatchHandlerTest {
    private final EventBusAdapter eventBusAdapter = mock();
    private final RoutingContextAdapter routingContextAdapter = mock();
    private final WatchHandler handler = new WatchHandler(eventBusAdapter);

    @Test
    public void shouldHandleWatchRequest() {
        final long lastEventId = System.currentTimeMillis();
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString());
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId);

        handler.handle(routingContextAdapter);

        final var sessionCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBusAdapter).registerDesignChangeNotificationConsumer(any());
        verify(eventBusAdapter).registerSessionUpdateNotificationConsumer(sessionCaptor.capture(), any());
        verifyNoMoreInteractions(eventBusAdapter);
        final var sessionId = sessionCaptor.getValue();

        verify(routingContextAdapter).getWatchKey();
        verify(routingContextAdapter).getRevision();
        verify(routingContextAdapter).getLastMessageId();
        verify(routingContextAdapter).initiateEventStreamResponse();
        verify(routingContextAdapter).writeEvent(eq("open"), eq(lastEventId), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("session")).isEqualTo(sessionId);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_0);
            softly.assertAll();
        }));
        verify(routingContextAdapter).setResponseCloseHandler(any());
        verifyNoMoreInteractions(routingContextAdapter);
    }

    @Test
    public void shouldHandleMultipleWatchRequests() {
        final long lastEventId1 = System.currentTimeMillis();
        final long lastEventId2 = lastEventId1 + 100;
        final long lastEventId3 = lastEventId1 + 200;
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString()).thenReturn(DESIGN_ID_2.toString()).thenReturn("*");
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0).thenReturn(REVISION_1).thenReturn(REVISION_2);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId1).thenReturn(lastEventId2).thenReturn(lastEventId3);

        handler.handle(routingContextAdapter);
        handler.handle(routingContextAdapter);
        handler.handle(routingContextAdapter);

        final var sessionCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBusAdapter).registerDesignChangeNotificationConsumer(any());
        verify(eventBusAdapter, times(3)).registerSessionUpdateNotificationConsumer(sessionCaptor.capture(), any());
        verifyNoMoreInteractions(eventBusAdapter);
        final var sessionId1 = sessionCaptor.getAllValues().get(0);
        final var sessionId2 = sessionCaptor.getAllValues().get(1);
        final var sessionId3 = sessionCaptor.getAllValues().get(2);

        verify(routingContextAdapter, times(3)).getWatchKey();
        verify(routingContextAdapter, times(3)).getRevision();
        verify(routingContextAdapter, times(3)).getLastMessageId();
        verify(routingContextAdapter, times(3)).initiateEventStreamResponse();
        verify(routingContextAdapter).writeEvent(eq("open"), eq(lastEventId1), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("session")).isEqualTo(sessionId1);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_0);
            softly.assertAll();
        }));
        verify(routingContextAdapter).writeEvent(eq("open"), eq(lastEventId2), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("session")).isEqualTo(sessionId2);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_1);
            softly.assertAll();
        }));
        verify(routingContextAdapter).writeEvent(eq("open"), eq(lastEventId3), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("session")).isEqualTo(sessionId3);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_2);
            softly.assertAll();
        }));
        verify(routingContextAdapter, times(3)).setResponseCloseHandler(any());
        verifyNoMoreInteractions(routingContextAdapter);
    }

    @Test
    public void shouldHandleDesignChangedNotification() {
        final var consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(eventBusAdapter).registerDesignChangeNotificationConsumer(consumerCaptor.capture());
        final var notificationConsumer = consumerCaptor.getValue();

        final long lastEventId = System.currentTimeMillis();
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString());
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId);

        handler.handle(routingContextAdapter);

        final var designChangedNotification = DesignChangedNotification.builder()
                .withKey(DESIGN_ID_1.toString())
                .withRevision(REVISION_1)
                .build();

        notificationConsumer.accept(designChangedNotification);

        final var sessionUpdatedNotification = SessionUpdatedNotification.builder()
                .withRevision(REVISION_1)
                .build();

        verify(eventBusAdapter).publishSessionUpdateNotification(any(), eq(sessionUpdatedNotification));
    }

    @Test
    public void shouldHandleMultipleDesignChangedNotifications() {
        final var consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(eventBusAdapter).registerDesignChangeNotificationConsumer(consumerCaptor.capture());
        final var notificationConsumer = consumerCaptor.getValue();

        final long lastEventId1 = System.currentTimeMillis();
        final long lastEventId2 = lastEventId1 + 100;
        final long lastEventId3 = lastEventId1 + 200;
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString()).thenReturn(DESIGN_ID_2.toString()).thenReturn("*");
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0).thenReturn(REVISION_1).thenReturn(REVISION_2);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId1).thenReturn(lastEventId2).thenReturn(lastEventId3);

        handler.handle(routingContextAdapter);
        handler.handle(routingContextAdapter);
        handler.handle(routingContextAdapter);

        final var sessionCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBusAdapter).registerDesignChangeNotificationConsumer(any());
        verify(eventBusAdapter, times(3)).registerSessionUpdateNotificationConsumer(sessionCaptor.capture(), any());
        verifyNoMoreInteractions(eventBusAdapter);
        final var sessionId1 = sessionCaptor.getAllValues().get(0);
        final var sessionId2 = sessionCaptor.getAllValues().get(1);
        final var sessionId3 = sessionCaptor.getAllValues().get(2);

        final var designChangedNotification1 = DesignChangedNotification.builder()
                .withKey(DESIGN_ID_1.toString())
                .withRevision(REVISION_1)
                .build();

        final var designChangedNotification2 = DesignChangedNotification.builder()
                .withKey(DESIGN_ID_2.toString())
                .withRevision(REVISION_2)
                .build();

        notificationConsumer.accept(designChangedNotification1);
        notificationConsumer.accept(designChangedNotification2);

        final var sessionUpdatedNotification1 = SessionUpdatedNotification.builder()
                .withRevision(REVISION_1)
                .build();

        final var sessionUpdatedNotification2 = SessionUpdatedNotification.builder()
                .withRevision(REVISION_2)
                .build();

        verify(eventBusAdapter).publishSessionUpdateNotification(eq(sessionId1), eq(sessionUpdatedNotification1));
        verify(eventBusAdapter).publishSessionUpdateNotification(eq(sessionId2), eq(sessionUpdatedNotification2));
        verify(eventBusAdapter).publishSessionUpdateNotification(eq(sessionId3), eq(sessionUpdatedNotification1));
        verify(eventBusAdapter).publishSessionUpdateNotification(eq(sessionId3), eq(sessionUpdatedNotification2));
    }

    @Test
    public void shouldHandleSessionUpdateNotification() {
        final long lastEventId = System.currentTimeMillis();
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString());
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId);

        handler.handle(routingContextAdapter);

        final var sessionCaptor = ArgumentCaptor.forClass(String.class);
        final var consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(eventBusAdapter).registerSessionUpdateNotificationConsumer(sessionCaptor.capture(), consumerCaptor.capture());
        final var sessionId = sessionCaptor.getValue();
        final var notificationConsumer = consumerCaptor.getValue();

        final var sessionUpdatedNotification = SessionUpdatedNotification.builder()
                .withRevision(REVISION_1)
                .build();

        notificationConsumer.accept(sessionUpdatedNotification);

        verify(routingContextAdapter).writeEvent(eq("update"), eq(lastEventId + 1), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("uuid")).isEqualTo(DESIGN_ID_1.toString());
            softly.assertThat(json.getString("session")).isEqualTo(sessionId);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_1);
            softly.assertAll();
        }));
    }

    @Test
    public void shouldHandleMultipleSessionUpdateNotifications() {
        final long lastEventId1 = System.currentTimeMillis();
        final long lastEventId2 = lastEventId1 + 100;
        final long lastEventId3 = lastEventId1 + 200;
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString()).thenReturn(DESIGN_ID_2.toString()).thenReturn("*");
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0).thenReturn(REVISION_1).thenReturn(REVISION_2);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId1).thenReturn(lastEventId2).thenReturn(lastEventId3);

        handler.handle(routingContextAdapter);
        handler.handle(routingContextAdapter);
        handler.handle(routingContextAdapter);

        final var sessionCaptor = ArgumentCaptor.forClass(String.class);
        final var consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(eventBusAdapter).registerDesignChangeNotificationConsumer(any());
        verify(eventBusAdapter, times(3)).registerSessionUpdateNotificationConsumer(sessionCaptor.capture(), consumerCaptor.capture());
        verifyNoMoreInteractions(eventBusAdapter);
        final var sessionId1 = sessionCaptor.getAllValues().get(0);
        final var sessionId2 = sessionCaptor.getAllValues().get(1);
        final var sessionId3 = sessionCaptor.getAllValues().get(2);
        final var notificationConsumer1 = consumerCaptor.getAllValues().get(0);
        final var notificationConsumer2 = consumerCaptor.getAllValues().get(1);
        final var notificationConsumer3 = consumerCaptor.getAllValues().get(2);

        final var sessionUpdatedNotification1 = SessionUpdatedNotification.builder()
                .withRevision(REVISION_1)
                .build();

        final var sessionUpdatedNotification2 = SessionUpdatedNotification.builder()
                .withRevision(REVISION_2)
                .build();

        notificationConsumer1.accept(sessionUpdatedNotification1);
        notificationConsumer2.accept(sessionUpdatedNotification2);
        notificationConsumer3.accept(sessionUpdatedNotification1);
        notificationConsumer3.accept(sessionUpdatedNotification2);

        verify(routingContextAdapter).writeEvent(eq("update"), eq(lastEventId1 + 1), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("uuid")).isEqualTo(DESIGN_ID_1.toString());
            softly.assertThat(json.getString("session")).isEqualTo(sessionId1);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_1);
            softly.assertAll();
        }));

        verify(routingContextAdapter).writeEvent(eq("update"), eq(lastEventId2 + 1), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("uuid")).isEqualTo(DESIGN_ID_2.toString());
            softly.assertThat(json.getString("session")).isEqualTo(sessionId2);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_2);
            softly.assertAll();
        }));

        verify(routingContextAdapter).writeEvent(eq("update"), eq(lastEventId3 + 1), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("uuid")).isEqualTo("*");
            softly.assertThat(json.getString("session")).isEqualTo(sessionId3);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_1);
            softly.assertAll();
        }));

        verify(routingContextAdapter).writeEvent(eq("update"), eq(lastEventId3 + 2), assertArg(message -> {
            SoftAssertions softly = new SoftAssertions();
            final var json = new JsonObject(message);
            softly.assertThat(json.getString("uuid")).isEqualTo("*");
            softly.assertThat(json.getString("session")).isEqualTo(sessionId3);
            softly.assertThat(json.getString("revision")).isEqualTo(REVISION_2);
            softly.assertAll();
        }));
    }

    @Test
    public void shouldHandleResponseClosed() {
        final long lastEventId = System.currentTimeMillis();
        when(routingContextAdapter.getWatchKey()).thenReturn(DESIGN_ID_1.toString());
        when(routingContextAdapter.getRevision()).thenReturn(REVISION_0);
        when(routingContextAdapter.getLastMessageId()).thenReturn(lastEventId);

        final var messageConsumerAdapter = mock(MessageConsumerAdapter.class);
        when(eventBusAdapter.registerSessionUpdateNotificationConsumer(any(), any())).thenReturn(messageConsumerAdapter);

        handler.handle(routingContextAdapter);

        final var consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(routingContextAdapter).setResponseCloseHandler(consumerCaptor.capture());
        final var closeConsumer = consumerCaptor.getValue();

        closeConsumer.accept(null);

        verify(messageConsumerAdapter).unregister();
    }
}