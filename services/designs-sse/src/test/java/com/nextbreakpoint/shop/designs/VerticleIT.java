package com.nextbreakpoint.shop.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.nextbreakpoint.shop.common.model.Message;
import com.nextbreakpoint.shop.common.model.MessageType;
import com.nextbreakpoint.shop.common.model.events.DesignChangedEvent;
import com.nextbreakpoint.shop.common.vertx.EventSource;
import com.nextbreakpoint.shop.common.vertx.KafkaClientFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TWO_SECONDS;

@Tag("slow")
@DisplayName("Verify contract of service Designs SSE")
public class VerticleIT {
    private static RestAssuredConfig restAssuredConfig;

    private Vertx vertx;
    private EventSource eventSource;
    private KafkaProducer<String, String> producer;

    @BeforeAll
    public static void configureRestAssured() {
        System.setProperty("crypto.policy", "unlimited");
        final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
        final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
        final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
    }

    @AfterAll
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @BeforeEach
    public void createVertx() {
        vertx = new Vertx(io.vertx.core.Vertx.vertx());
    }

    @AfterEach
    public void destroyVertx() {
        vertx.close();
    }

    @BeforeEach
    public void createEventSource() {
        final JsonObject config = new JsonObject();
        config.put("client_keep_alive", true);
        config.put("client_verify_host", false);
        config.put("client_keystore_path", "../secrets/keystore-client.jks");
        config.put("client_keystore_secret", "secret");
        config.put("client_truststore_path", "../secrets/truststore-client.jks");
        config.put("client_truststore_secret", "secret");
        final Integer port = Integer.getInteger("http.port", 3041);
        eventSource = new EventSource(vertx, "https://localhost:" + port, config);
    }

    @AfterEach
    public void destroyEventSource() {
        if (eventSource != null) {
            eventSource.close();
        }
    }

    @BeforeEach
    public void createPoducer() {
        producer = KafkaClientFactory.createProducer(vertx, createProducerConfig());
    }

    @AfterEach
    public void destroyProducer() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    @DisplayName("Should notify watchers after receiving a DesignChanged event")
    public void shouldNotifyWatchersWhenRecevingAnEvent() throws IOException {
        long eventTimestamp = System.currentTimeMillis();

        final UUID messageId = UUID.randomUUID();
        final UUID designId = UUID.randomUUID();

        final DesignChangedEvent designChangedEvent = new DesignChangedEvent(designId, eventTimestamp);

        final long messageTimestamp = System.currentTimeMillis();

        final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

        final Boolean[] connected = new Boolean[]{null};
        final String[] open = new String[]{null};
        final String[] update = new String[]{null};

        eventSource.connect("/api/designs/events/0", null, result -> {
            connected[0] = result.succeeded();
            producer.rxWrite(createKafkaRecord(designChangedMessage)).subscribe();
        }).onMessage(sseMessage -> {
        }).onEvent("update", sseEvent -> {
            update[0] = sseEvent;
            System.out.println(sseEvent);
        }).onEvent("open", sseEvent -> {
            open[0] = sseEvent;
            System.out.println(sseEvent);
        }).onClose(nothing -> {});

        await().atMost(TWO_SECONDS)
                .pollInterval(ONE_HUNDRED_MILLISECONDS)
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
    }

    @Test
    @DisplayName("Should notify watchers after receiving a DesignChanged event for single design")
    public void shouldNotifyWatchersWhenRecevingAnEventForSingleDesign() throws IOException {
        long eventTimestamp = System.currentTimeMillis();

        final UUID messageId = UUID.randomUUID();
        final UUID designId = UUID.randomUUID();

        final DesignChangedEvent designChangedEvent = new DesignChangedEvent(designId, eventTimestamp);

        final long messageTimestamp = System.currentTimeMillis();

        final Message designChangedMessage = createDesignChangedMessage(messageId, designId, messageTimestamp, designChangedEvent);

        final Boolean[] connected = new Boolean[]{null};
        final String[] open = new String[]{null};
        final String[] update = new String[]{null};

        eventSource.connect("/api/designs/events/0/" + designId, null, result -> {
            connected[0] = result.succeeded();
            producer.rxWrite(createKafkaRecord(designChangedMessage)).subscribe();
        }).onMessage(sseMessage -> {
        }).onEvent("update", sseEvent -> {
            update[0] = sseEvent;
            System.out.println(sseEvent);
        }).onEvent("open", sseEvent -> {
            open[0] = sseEvent;
            System.out.println(sseEvent);
        }).onClose(nothing -> {});

        await().atMost(TWO_SECONDS)
                .pollInterval(ONE_HUNDRED_MILLISECONDS)
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
    }

    private JsonObject createProducerConfig() {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrapServers", System.getProperty("kafka.host", "localhost") + ":9092");
        return config;
    }

    private KafkaProducerRecord<String, String> createKafkaRecord(Message message) {
        return KafkaProducerRecord.create("designs-sse", message.getPartitionKey(), Json.encode(message));
    }

    private Message createDesignChangedMessage(UUID messageId, UUID partitionKey, long timestamp, DesignChangedEvent event) {
        return new Message(messageId.toString(), MessageType.DESIGN_CHANGED, Json.encode(event), "test", partitionKey.toString(), timestamp);
    }

    private URL makeBaseURL(String path) throws MalformedURLException {
        final Integer port = Integer.getInteger("http.port", 3041);
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        final String host = System.getProperty("http.host", "localhost");
        return new URL("https://" + host + ":" + port + "/" + normPath);
    }
}
