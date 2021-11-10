package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.test.EventSource;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class IntegrationTests {
    private static final TestScenario scenario = new TestScenario();

    private static Environment environment = Environment.getDefaultEnvironment();

    private static final List<SSENotification> notifications = Collections.synchronizedList(new ArrayList<>());
    private static KafkaProducer<String, String> producer;
    private static EventSource eventSource;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

        producer = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

        eventSource = new EventSource(environment, vertx, "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort(), scenario.getEventSourceConfig());
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        if (producer != null) {
            try {
                producer.close();
            } catch (Exception ignore) {
            }
        }

        if (eventSource != null) {
            eventSource.close();
        }

        scenario.after();
    }

    @Nested
    @Tag("slow")
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-notification-dispatcher service")
    public class VerifyService {
        @Test
        @DisplayName("Should notify watchers after receiving a DesignChanged event")
        public void shouldNotifyWatchersWhenReceivingAnEvent() throws IOException {
            try {
                long eventTimestamp = System.currentTimeMillis();

                final UUID messageId = UUID.randomUUID();
                final UUID designId = UUID.randomUUID();

                final DesignChanged designChangedEvent = new DesignChanged(designId, eventTimestamp);

                final long messageTimestamp = System.currentTimeMillis();

                final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

                notifications.clear();

                eventSource.connect("/v1/sse/designs/0", null, result -> {
                    notifications.add(new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE"));
                    producer.send(createKafkaRecord(designChangedMessage));
                }).onEvent("update", sseEvent -> {
                    notifications.add(new SSENotification("UPDATE", sseEvent));
                }).onEvent("open", sseEvent -> {
                    notifications.add(new SSENotification("OPEN", sseEvent));
                }).onClose(nothing -> {
                });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            assertThat(notifications).isNotEmpty();
                            List<SSENotification> events = new ArrayList<>(notifications);
                            events.forEach(System.out::println);
                            assertThat(notifications).hasSize(3);
                            assertThat(events.get(0).type).isEqualTo("CONNECT");
                            assertThat(events.get(1).type).isEqualTo("OPEN");
                            assertThat(events.get(2).type).isEqualTo("UPDATE");
                            String openData = events.get(1).body.split("\n")[1];
                            Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                            assertThat(openObject.get("session")).isNotNull();
                            String updateData = events.get(2).body.split("\n")[1];
                            Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                            assertThat(updateObject.get("session")).isNotNull();
                            assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                            assertThat(updateObject.get("uuid")).isNotNull();
                            assertThat(updateObject.get("uuid")).isEqualTo("*");
                        });
            } finally {
                eventSource.close();
            }
        }

        @Test
        @DisplayName("Should notify watchers after receiving a DesignChanged event for single design")
        public void shouldNotifyWatchersWhenReceivingAnEventForSingleDesign() throws IOException {
            try {
                long eventTimestamp = System.currentTimeMillis();

                final UUID messageId = UUID.randomUUID();
                final UUID designId = UUID.randomUUID();

                final DesignChanged designChangedEvent = new DesignChanged(designId, eventTimestamp);

                final long messageTimestamp = System.currentTimeMillis();

                final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

                notifications.clear();

                eventSource.connect("/v1/sse/designs/0/" + designId, null, result -> {
                    notifications.add(new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE"));
                    producer.send(createKafkaRecord(designChangedMessage));
                }).onEvent("update", sseEvent -> {
                    notifications.add(new SSENotification("UPDATE", sseEvent));
                }).onEvent("open", sseEvent -> {
                    notifications.add(new SSENotification("OPEN", sseEvent));
                }).onClose(nothing -> {
                });

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            assertThat(notifications).isNotEmpty();
                            List<SSENotification> events = new ArrayList<>(notifications);
                            events.forEach(System.out::println);
                            assertThat(notifications).hasSize(3);
                            assertThat(events.get(0).type).isEqualTo("CONNECT");
                            assertThat(events.get(1).type).isEqualTo("OPEN");
                            assertThat(events.get(2).type).isEqualTo("UPDATE");
                            String openData = events.get(1).body.split("\n")[1];
                            Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                            assertThat(openObject.get("session")).isNotNull();
                            String updateData = events.get(2).body.split("\n")[1];
                            Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                            assertThat(updateObject.get("session")).isNotNull();
                            assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                            assertThat(updateObject.get("uuid")).isNotNull();
                            assertThat(updateObject.get("uuid")).isEqualTo(designId.toString());
                        });
            } finally {
                eventSource.close();
            }
        }
    }

    private static ProducerRecord<String, String> createKafkaRecord(Message message) {
        return new ProducerRecord<>("design-event", message.getKey(), Json.encode(message.getPayload()));
    }

    private static Message createDesignChangedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChanged event) {
        return new Message(partitionKey.toString(), 0, timestamp,  new Payload(messageId, MessageType.DESIGN_CHANGED, Json.encode(event), "test"));
    }

    private static class SSENotification {
        public final String type;
        public final String body;

        public SSENotification(String type, String body) {
            this.type = type;
            this.body = body;
        }

        @Override
        public String toString() {
            return type + ": " + body;
        }
    }
}
