package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.test.EventSource;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

@Tag("slow")
public class TestSuite {
    private static final TestScenario scenario = new TestScenario();

    private static Environment environment = Environment.getDefaultEnvironment();

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        scenario.after();
    }

    @Nested
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-notification-dispatcher service")
    public class VerifyServiceIntegration {
        private EventSource eventSource;

        @BeforeEach
        public void setup() {
            final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());
            final JsonObject config = new JsonObject();
            config.put("client_keep_alive", "true");
            config.put("client_verify_host", "false");
            config.put("client_keystore_path", "../../secrets/keystore_client.jks");
            config.put("client_keystore_secret", "secret");
            config.put("client_truststore_path", "../../secrets/truststore_client.jks");
            config.put("client_truststore_secret", "secret");
            eventSource = new EventSource(environment, vertx, "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort(), config);
        }

        @AfterEach
        public void reset() {
            if (eventSource != null) {
                eventSource.close();
            }
        }

        @Test
        @DisplayName("Should notify watchers after receiving a DesignChanged event")
        public void shouldNotifyWatchersWhenRecevingAnEvent() throws IOException {
            final KafkaProducer<String, String> producer[] = new KafkaProducer[1];

            try {
                producer[0] = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

                long eventTimestamp = System.currentTimeMillis();

                final UUID messageId = UUID.randomUUID();
                final UUID designId = UUID.randomUUID();

                final DesignChanged designChangedEvent = new DesignChanged(designId, eventTimestamp);

                final long messageTimestamp = System.currentTimeMillis();

                final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

                final Boolean[] connected = new Boolean[]{null};
                final String[] open = new String[]{null};
                final String[] update = new String[]{null};

                eventSource.connect("/v1/sse/designs/0", null, result -> {
                    connected[0] = result.succeeded();
                    producer[0].send(createKafkaRecord(designChangedMessage));
                }).onEvent("update", sseEvent -> {
                    update[0] = sseEvent;
                    System.out.println(sseEvent);
                }).onEvent("open", sseEvent -> {
                    open[0] = sseEvent;
                    System.out.println(sseEvent);
                }).onClose(nothing -> {});

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            assertThat(connected[0]).isNotNull();
                            assertThat(connected[0]).isTrue();
                            assertThat(open[0]).isNotNull();
                            String openData = open[0].split("\n")[1];
                            Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                            assertThat(openObject.get("session")).isNotNull();
                            assertThat(update[0]).isNotNull();
                            String updateData = update[0].split("\n")[1];
                            Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                            assertThat(updateObject.get("session")).isNotNull();
                            assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                            assertThat(updateObject.get("uuid")).isNotNull();
                            assertThat(updateObject.get("uuid")).isEqualTo("*");
                        });
            } finally {
                if (producer[0] != null) {
                    producer[0].close();
                }
            }
        }

        @Test
        @DisplayName("Should notify watchers after receiving a DesignChanged event for single design")
        public void shouldNotifyWatchersWhenRecevingAnEventForSingleDesign() throws IOException {
            final KafkaProducer<String, String> producer[] = new KafkaProducer[1];

            try {
                producer[0] = KafkaUtils.createProducer(environment, scenario.createProducerConfig());

                long eventTimestamp = System.currentTimeMillis();

                final UUID messageId = UUID.randomUUID();
                final UUID designId = UUID.randomUUID();

                final DesignChanged designChangedEvent = new DesignChanged(designId, eventTimestamp);

                final long messageTimestamp = System.currentTimeMillis();

                final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

                final Boolean[] connected = new Boolean[]{null};
                final String[] open = new String[]{null};
                final String[] update = new String[]{null};

                eventSource.connect("/v1/sse/designs/0/" + designId, null, result -> {
                    connected[0] = result.succeeded();
                    producer[0].send(createKafkaRecord(designChangedMessage));
                }).onEvent("update", sseEvent -> {
                    update[0] = sseEvent;
                    System.out.println(sseEvent);
                }).onEvent("open", sseEvent -> {
                    open[0] = sseEvent;
                    System.out.println(sseEvent);
                }).onClose(nothing -> {});

                await().atMost(TEN_SECONDS)
                        .pollInterval(ONE_SECOND)
                        .untilAsserted(() -> {
                            assertThat(connected[0]).isNotNull();
                            assertThat(connected[0]).isTrue();
                            assertThat(open[0]).isNotNull();
                            String openData = open[0].split("\n")[1];
                            Map<String, Object> openObject = Json.decodeValue(openData, HashMap.class);
                            assertThat(openObject.get("session")).isNotNull();
                            assertThat(update[0]).isNotNull();
                            String updateData = update[0].split("\n")[1];
                            Map<String, Object> updateObject = Json.decodeValue(updateData, HashMap.class);
                            assertThat(updateObject.get("session")).isNotNull();
                            assertThat(updateObject.get("session")).isEqualTo(openObject.get("session"));
                            assertThat(updateObject.get("uuid")).isNotNull();
                            assertThat(updateObject.get("uuid")).isEqualTo(designId.toString());
                        });
            } finally {
                if (producer[0] != null) {
                    producer[0].close();
                }
            }
        }

        private ProducerRecord<String, String> createKafkaRecord(Message message) {
            return new ProducerRecord<>("designs-sse", message.getPartitionKey(), Json.encode(message));
        }

        private Message createDesignChangedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChanged event) {
            return new Message(messageId.toString(), MessageType.DESIGN_CHANGED, Json.encode(event), "test", partitionKey.toString(), timestamp);
        }
    }
}
