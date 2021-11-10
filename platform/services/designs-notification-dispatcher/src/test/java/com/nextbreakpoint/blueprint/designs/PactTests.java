package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.test.EventSource;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import com.nextbreakpoint.blueprint.designs.model.DesignChanged;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class PactTests {
    private static final String UUID_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private static final UUID DESIGN_UUID_1 = new UUID(0L, 1L);
    private static final UUID DESIGN_UUID_2 = new UUID(0L, 2L);

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
    @Tag("pact")
    @DisplayName("Test designs-notification-dispatcher pact")
    @ExtendWith(PactConsumerTestExt.class)
    public class TestDesignsNotificationDispatcher {
        @Pact(consumer = "designs-notification-dispatcher")
        public MessagePact designChanged(MessagePactBuilder builder) {
            PactDslJsonBody body1 = new PactDslJsonBody()
                    .stringValue("uuid", DESIGN_UUID_1.toString())
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_1.toString())
                    .stringValue("messageType", "design-insert")
                    .stringValue("messageBody", body1.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody body2 = new PactDslJsonBody()
                    .stringValue("uuid", DESIGN_UUID_2.toString())
                    .stringMatcher("timestamp", "\\d{10}");

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringMatcher("messageId", DESIGN_UUID_2.toString())
                    .stringValue("messageType", "design-insert")
                    .stringValue("messageBody", body2.toString())
                    .stringValue("messageSource", "service-designs")
                    .stringMatcher("partitionKey", UUID_REGEXP)
                    .stringMatcher("timestamp", "\\d{10}");

            return builder.given("kafka topic exists")
                    .expectsToReceive("design changed event 1")
                    .withContent(message1)
                    .expectsToReceive("design changed event 2")
                    .withContent(message2)
                    .toPact();
        }

        @Test
        @PactTestFor(providerName = "designs-command-consumer", port = "1111", pactMethod = "designChanged", providerType = ProviderType.ASYNCH)
        public void shouldProduceNotificationWhenADesignChanged(MessagePact messagePact) throws MalformedURLException {
            try {
                final InputMessage designChangedMessage1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), InputMessage.class);

                final InputMessage designChangedMessage2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), InputMessage.class);

                final DesignChanged event1 = Json.decodeValue(designChangedMessage1.getValue().getData(), DesignChanged.class);

                final DesignChanged event2 = Json.decodeValue(designChangedMessage2.getValue().getData(), DesignChanged.class);

                long eventTimestamp1 = System.currentTimeMillis() - 2;

                long eventTimestamp2 = System.currentTimeMillis() - 1;

                final UUID designId1 = event1.getUuid();

                final UUID designId2 = event2.getUuid();

                final DesignChanged designChangedEvent1 = new DesignChanged(designId1, eventTimestamp1);

                final DesignChanged designChangedEvent2 = new DesignChanged(designId2, eventTimestamp2);

                final long messageTimestamp = System.currentTimeMillis();

                final OutputMessage newDesignChangedMessage1 = createDesignChangedMessage(UUID.randomUUID(), designId1, messageTimestamp, designChangedEvent1);

                final OutputMessage newDesignChangedMessage2 = createDesignChangedMessage(UUID.randomUUID(), designId2, messageTimestamp, designChangedEvent2);

                notifications.clear();

                eventSource.connect("/v1/sse/designs/0/" + designId1, null, result -> {
                    notifications.add(new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE"));
                    producer.send(createKafkaRecord(newDesignChangedMessage1));
                    producer.send(createKafkaRecord(newDesignChangedMessage2));
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
                            assertThat(updateObject.get("uuid")).isEqualTo(designId1.toString());
                        });
            } finally {
                eventSource.close();
            }
        }

        @Test
        @PactTestFor(providerName = "designs-command-consumer", port = "1112", pactMethod = "designChanged", providerType = ProviderType.ASYNCH)
        public void shouldProduceNotificationWhenAnyDesignChanged(MessagePact messagePact) throws MalformedURLException {
            try {
                final InputMessage designChangedMessage1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), InputMessage.class);

                final InputMessage designChangedMessage2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), InputMessage.class);

                final DesignChanged event1 = Json.decodeValue(designChangedMessage1.getValue().getData(), DesignChanged.class);

                final DesignChanged event2 = Json.decodeValue(designChangedMessage2.getValue().getData(), DesignChanged.class);

                long eventTimestamp1 = System.currentTimeMillis() - 2;

                long eventTimestamp2 = System.currentTimeMillis() - 1;

                final UUID designId1 = event1.getUuid();

                final UUID designId2 = event2.getUuid();

                final DesignChanged designChangedEvent1 = new DesignChanged(designId1, eventTimestamp1);

                final DesignChanged designChangedEvent2 = new DesignChanged(designId2, eventTimestamp2);

                final long messageTimestamp = System.currentTimeMillis();

                final OutputMessage newDesignChangedMessage1 = createDesignChangedMessage(UUID.randomUUID(), designId1, messageTimestamp, designChangedEvent1);

                final OutputMessage newDesignChangedMessage2 = createDesignChangedMessage(UUID.randomUUID(), designId2, messageTimestamp, designChangedEvent2);

                notifications.clear();

                eventSource.connect("/v1/sse/designs/0", null, result -> {
                    notifications.add(new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE"));
                    producer.send(createKafkaRecord(newDesignChangedMessage1));
                    producer.send(createKafkaRecord(newDesignChangedMessage2));
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
    }

    private static ProducerRecord<String, String> createKafkaRecord(OutputMessage message) {
        return new ProducerRecord<>("design-event", message.getKey(), Json.encode(message.getValue()));
    }

    private static OutputMessage createDesignChangedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChanged event) {
        return new OutputMessage(partitionKey.toString(), new Payload(messageId, MessageType.DESIGN_CHANGED, Json.encode(event), "test"));
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
