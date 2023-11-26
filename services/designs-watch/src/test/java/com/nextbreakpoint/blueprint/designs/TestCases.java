package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.test.EventSource;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.common.vertx.HttpClientConfig;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final TestContext context = new TestContext();

    private final TestSteps steps = new TestSteps(context, new TestActionsImpl());

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private EventSource eventSource;

    private KafkaTestEmitter<Object, Payload> eventEmitter;

    public TestCases() {}

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        KafkaProducer<String, Payload> producer = KafkaClientFactory.createProducer(createProducerConfig("integration"));

        eventEmitter = new KafkaTestEmitter<>(producer, Records.createEventOutputRecordMapper(), TestConstants.EVENTS_TOPIC_NAME + "-" + scenario.getUniqueTestId());

        eventSource = new EventSource(vertx, getServiceUrl(), getEventSourceConfig());

        context.clear();
    }

    public void after() {
        eventSource.close();

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
    public TestSteps getSteps() {
        return steps;
    }

    @NotNull
    public String getServiceUrl() {
        return "http://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    private KafkaProducerConfig createProducerConfig(String clientId) {
        return KafkaProducerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withSchemaRegistryUrl("http://" + scenario.getSchemaRegistryHost() + ":" + scenario.getSchemaRegistryPort())
                .withAutoRegisterSchemas(true)
                .withKeySerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withValueSerializer("io.confluent.kafka.serializers.KafkaAvroSerializer")
                .withClientId(clientId)
                .withKafkaAcks("1")
                .build();
    }

    @NotNull
    private HttpClientConfig getEventSourceConfig() {
        return HttpClientConfig.builder()
                .withKeepAlive(Boolean.TRUE)
                .withVerifyHost(Boolean.FALSE)
//                .withKeyStorePath("../../secrets/keystore_client.jks")
//                .withKeyStoreSecret("secret")
//                .withTrustStorePath("../../secrets/truststore_client.jks")
//                .withTrustStoreSecret("secret")
                .build();
    }

    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(List<OutputMessage<DesignDocumentUpdateCompleted>> designDocumentUpdateCompletedMessages) {
        getSteps()
                .given().theDesignDocumentUpdateCompletedMessage(designDocumentUpdateCompletedMessages.get(0))
                .when().discardMessages()
                .and().appendMessage()
                .given().theDesignDocumentUpdateCompletedMessage(designDocumentUpdateCompletedMessages.get(1))
                .when().appendMessage()
                .then().shouldNotifyWatchersOfAllResources();
    }

    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(List<OutputMessage<DesignDocumentUpdateCompleted>> designDocumentUpdateCompletedMessages, UUID designId) {
        getSteps()
                .given().theDesignDocumentUpdateCompletedMessage(designDocumentUpdateCompletedMessages.get(0))
                .when().discardMessages()
                .and().appendMessage()
                .given().theDesignDocumentUpdateCompletedMessage(designDocumentUpdateCompletedMessages.get(1))
                .when().appendMessage()
                .then().shouldNotifyWatchersOfAResource(designId);
    }

    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent(List<OutputMessage<DesignDocumentDeleteCompleted>> designDocumentDeleteCompletedMessages) {
        getSteps()
                .given().theDesignDocumentDeleteCompletedMessage(designDocumentDeleteCompletedMessages.get(0))
                .when().discardMessages()
                .and().appendMessage()
                .given().theDesignDocumentDeleteCompletedMessage(designDocumentDeleteCompletedMessages.get(1))
                .when().appendMessage()
                .then().shouldNotifyWatchersOfAllResources();
    }

    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent(List<OutputMessage<DesignDocumentDeleteCompleted>> designDocumentDeleteCompletedMessages, UUID designId) {
        getSteps()
                .given().theDesignDocumentDeleteCompletedMessage(designDocumentDeleteCompletedMessages.get(0))
                .when().discardMessages()
                .and().appendMessage()
                .given().theDesignDocumentDeleteCompletedMessage(designDocumentDeleteCompletedMessages.get(1))
                .when().appendMessage()
                .then().shouldNotifyWatchersOfAResource(designId);
    }

    public static class SSENotification {
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

    private class TestActionsImpl implements TestActions {
        @Override
        public void emitMessage(Source source, OutputMessage<Object> message, Function<String, String> topicMapper) {
            emitter(source).send(message, topicMapper.apply(emitter(source).getTopicName()));
        }

        @Override
        public List<InputMessage<Object>> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage<Object>> messagePredicate) {
            return null;
        }

        @Override
        public void subscribe(List<SSENotification> notifications, Runnable callback) {
            try {
                eventSource
                        .onClose(nothing -> System.out.println("closed"))
                        .onEvent("update", sseEvent -> {
                            final SSENotification notification = new SSENotification("UPDATE", sseEvent);
                            System.out.println(notification);
                            notifications.add(notification);
                        })
                        .onEvent("open", sseEvent -> {
                            final SSENotification notification = new SSENotification("OPEN", sseEvent);
                            System.out.println(notification);
                            notifications.add(notification);
                        })
                        .connect("/v1/designs/watch?revision=0000000000000000-0000000000000000", null, result -> {
                            final SSENotification notification = new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE");
                            System.out.println(notification);
                            notifications.add(notification);
                            if (result.succeeded()) {
                                callback.run();
                            }
                        });
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void subscribe(List<SSENotification> notifications, Runnable callback, UUID designId) {
            try {
                eventSource
                        .onClose(nothing -> System.out.println("closed"))
                        .onEvent("update", sseEvent -> {
                            final SSENotification notification = new SSENotification("UPDATE", sseEvent);
                            System.out.println(notification);
                            notifications.add(notification);
                        })
                        .onEvent("open", sseEvent -> {
                            final SSENotification notification = new SSENotification("OPEN", sseEvent);
                            System.out.println(notification);
                            notifications.add(notification);
                        })
                        .connect("/v1/designs/watch?designId=" + designId + "&revision=0000000000000000-0000000000000000", null, result -> {
                            final SSENotification notification = new SSENotification("CONNECT", result.succeeded() ? "SUCCESS" : "FAILURE");
                            System.out.println(notification);
                            notifications.add(notification);
                            if (result.succeeded()) {
                                callback.run();
                            }
                        });
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void unsubscribe() {
            eventSource.close();
        }

        private KafkaTestEmitter<Object, Payload> emitter(Source source) {
            return switch (source) {
                case EVENTS -> eventEmitter;
            };
        }
    }
}
