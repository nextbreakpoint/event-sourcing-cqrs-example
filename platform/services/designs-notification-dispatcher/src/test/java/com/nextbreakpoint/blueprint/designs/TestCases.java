package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.test.EventSource;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private final Environment environment = Environment.getDefaultEnvironment();

    private EventSource eventSource;

    private KafkaTestEmitter eventEmitter;

    private List<SSENotification> notifications = Collections.synchronizedList(new ArrayList<>());

    public TestCases() {}

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, createProducerConfig());

        eventEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);

        eventSource = new EventSource(environment, vertx, getServiceUrl(), getEventSourceConfig());
    }

    public void after() {
        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toCompletable()
                    .await();
        } catch (Exception ignore) {
        }

        scenario.after();
    }

    @NotNull
    public String getVersion() {
        return scenario.getVersion();
    }

    @NotNull
    public String getServiceUrl() {
        return "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public JsonObject createProducerConfig() {
        final JsonObject config = new JsonObject();
        config.put("kafka_bootstrap_servers", scenario.getKafkaHost() + ":" + scenario.getKafkaPort());
        config.put("kafka_client_id", "integration");
        return config;
    }

    @NotNull
    public JsonObject getEventSourceConfig() {
        final JsonObject config = new JsonObject();
        config.put("client_keep_alive", "true");
        config.put("client_verify_host", "false");
        config.put("client_keystore_path", "../../secrets/keystore_client.jks");
        config.put("client_keystore_secret", "secret");
        config.put("client_truststore_path", "../../secrets/truststore_client.jks");
        config.put("client_truststore_secret", "secret");
        return config;
    }

    public void shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List<OutputMessage> messages) {
        try {
            notifications.clear();

            eventSource.connect("/v1/sse/designs/0", null, result -> {
                final SSENotification notification = new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE");
                System.out.println(notification);
                notifications.add(notification);
                if (result.succeeded()) {
                    eventEmitter.sendAsync(messages.get(0));
                    eventEmitter.sendAsync(messages.get(1));
                }
            }).onEvent("update", sseEvent -> {
                final SSENotification notification = new SSENotification("UPDATE", sseEvent);
                System.out.println(notification);
                notifications.add(notification);
            }).onEvent("open", sseEvent -> {
                final SSENotification notification = new SSENotification("OPEN", sseEvent);
                System.out.println(notification);
                notifications.add(notification);
            }).onClose(nothing -> {
                System.out.println("closed");
            });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        assertThat(notifications).isNotEmpty();
                        List<SSENotification> events = new ArrayList<>(notifications);
//                        events.forEach(System.out::println);
                        assertThat(notifications).hasSize(4);
                        assertThat(events.get(0).type).isEqualTo("CONNECT");
                        assertThat(events.get(1).type).isEqualTo("OPEN");
                        assertThat(events.get(2).type).isEqualTo("UPDATE");
                        assertThat(events.get(3).type).isEqualTo("UPDATE");
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
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } finally {
            eventSource.close();
        }
    }

    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List<OutputMessage> messages) {
        try {
            final String designId = messages.get(0).getKey();

            notifications.clear();

            eventSource.connect("/v1/sse/designs/0/" + designId, null, result -> {
                final SSENotification notification = new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE");
                System.out.println(notification);
                notifications.add(notification);
                if (result.succeeded()) {
                    eventEmitter.sendAsync(messages.get(0));
                    eventEmitter.sendAsync(messages.get(1));
                }
            }).onEvent("update", sseEvent -> {
                final SSENotification notification = new SSENotification("UPDATE", sseEvent);
                System.out.println(notification);
                notifications.add(notification);
            }).onEvent("open", sseEvent -> {
                final SSENotification notification = new SSENotification("OPEN", sseEvent);
                System.out.println(notification);
                notifications.add(notification);
            }).onClose(nothing -> {
                System.out.println("closed");
            });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        assertThat(notifications).isNotEmpty();
                        List<SSENotification> events = new ArrayList<>(notifications);
//                        events.forEach(System.out::println);
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
                        assertThat(updateObject.get("uuid")).isEqualTo(designId);
                    });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } finally {
            eventSource.close();
        }
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
