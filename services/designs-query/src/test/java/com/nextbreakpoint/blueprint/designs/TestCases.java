package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.designs.model.Design;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final TestContext context = new TestContext();

    private final TestSteps steps = new TestSteps(context, new TestActionsImpl());

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling<Object, Payload> eventsPolling;
    private KafkaTestEmitter<Object, Payload> eventEmitter;

    private TestElasticsearch testElasticsearch;
    private TestElasticsearch testDraftElasticsearch;

    private S3Client s3Client;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId + "-" + scenario.getUniqueTestId();
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        TestS3.deleteContent(s3Client, TestConstants.BUCKET, object -> true);
        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
        TestS3.createBucket(s3Client, TestConstants.BUCKET);

        KafkaProducer<String, Payload> producer = KafkaClientFactory.createProducer(createProducerConfig("integration"));

        KafkaConsumer<String, Payload> eventsConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        eventsPolling = new KafkaTestPolling<>(eventsConsumer, Records.createEventInputRecordMapper(), TestConstants.EVENTS_TOPIC_NAME + "-" + scenario.getUniqueTestId());

        eventsPolling.startPolling();

        eventEmitter = new KafkaTestEmitter<>(producer, Records.createEventOutputRecordMapper(), TestConstants.EVENTS_TOPIC_NAME + "-" + scenario.getUniqueTestId());

        final RestClient restClient = RestClient.builder(new HttpHost(scenario.getElasticsearchHost(), scenario.getElasticsearchPort())).build();

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());

        final ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));

        testElasticsearch = new TestElasticsearch(new ElasticsearchClient(transport), TestConstants.DESIGNS_INDEX_NAME);
        testDraftElasticsearch = new TestElasticsearch(new ElasticsearchClient(transport), TestConstants.DESIGNS_INDEX_NAME + "_draft");

        deleteData();

        context.clear();
    }

    public void after() {
        if (eventsPolling != null) {
            eventsPolling.stopPolling();
        }

        try {
            vertx.rxClose()
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toCompletable()
                    .await();
        } catch (Exception ignore) {
        }

        try {
            if (s3Client != null) {
                s3Client.close();
            }
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

    public void deleteData() {
        deleteDesigns();
        deleteDraftDesigns();
    }

    @NotNull
    public URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("http://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
    }

    @NotNull
    public String makeAuthorization(String user, String role) {
        return scenario.makeAuthorization(user, role);
    }

    @NotNull
    public String getOriginUrl() {
        return "http://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public HttpTestTarget getHttpTestTarget() {
        return new HttpTestTarget(scenario.getServiceHost(), scenario.getServicePort(), "/");
    }

    @NotNull
    private KafkaConsumerConfig createConsumerConfig(String groupId) {
        return KafkaConsumerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withSchemaRegistryUrl("http://" + scenario.getSchemaRegistryHost() + ":" + scenario.getSchemaRegistryPort())
                .withSpecificAvroReader(true)
                .withAutoRegisterSchemas(false)
                .withKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withValueDeserializer("io.confluent.kafka.serializers.KafkaAvroDeserializer")
                .withAutoOffsetReset("earliest")
                .withEnableAutoCommit("false")
                .withGroupId(groupId)
                .build();
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

    public void deleteDraftDesigns() {
        // elasticsearch client detects the class io.vertx.tracing.opentelemetry.VertxContextStorageProvider as
        // open telemetry provider, therefore we need a vertx context otherwise the elasticsearch client fails
        vertx.getOrCreateContext().executeBlocking(() -> {
            testDraftElasticsearch.deleteDesigns();
            return null;
        })
        .onFailure(Throwable::printStackTrace)
        .toCompletionStage()
        .toCompletableFuture()
        .join();
    }

    public void insertDraftDesign(Design design) {
        vertx.getOrCreateContext().executeBlocking(() -> {
            testDraftElasticsearch.insertDesign(design);
            return null;
        })
        .onFailure(Throwable::printStackTrace)
        .toCompletionStage()
        .toCompletableFuture()
        .join();

        final byte[] data = TestUtils.makeImage(256);

        TestUtils.generateKeys(design)
                .doOnNext(key -> TestS3.putObject(s3Client, TestConstants.BUCKET, key, data))
                .ignoreElements()
                .toCompletable()
                .await();
    }

    public void deleteDesigns() {
        // elasticsearch client detects the class io.vertx.tracing.opentelemetry.VertxContextStorageProvider as
        // open telemetry provider, therefore we need a vertx context otherwise the elasticsearch client fails
        vertx.getOrCreateContext().executeBlocking(() -> {
            testElasticsearch.deleteDesigns();
            return null;
        })
        .onFailure(Throwable::printStackTrace)
        .toCompletionStage()
        .toCompletableFuture()
        .join();
    }

    public void insertDesign(Design design) {
        vertx.getOrCreateContext().executeBlocking(() -> {
            testElasticsearch.insertDesign(design);
            return null;
        })
        .onFailure(Throwable::printStackTrace)
        .toCompletionStage()
        .toCompletableFuture()
        .join();

        final byte[] data = TestUtils.makeImage(256);

        TestUtils.generateKeys(design)
                .doOnNext(key -> TestS3.putObject(s3Client, TestConstants.BUCKET, key, data))
                .ignoreElements()
                .toCompletable()
                .await();
    }

    public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequested(List<OutputMessage<DesignDocumentUpdateRequested>> designDocumentUpdateRequestedMessages) {
        getSteps()
                .given().theDesignDocumentUpdateRequestedMessage(designDocumentUpdateRequestedMessages.get(0))
                .when().publishTheMessage()
                .then().aDocumentUpdateCompletedMessageShouldBePublished()
                .and().theDocumentUpdateCompletedEventShouldHaveExpectedValues()
                .and().theDraftDesignDocumentShouldBeUpdated()
                .and().theDesignDocumentShouldNotExist()
                .given().theDesignDocumentUpdateRequestedMessage(designDocumentUpdateRequestedMessages.get(1))
                .when().publishTheMessage()
                .then().aDocumentUpdateCompletedMessageShouldBePublished()
                .and().theDocumentUpdateCompletedEventShouldHaveExpectedValues()
                .and().theDraftDesignDocumentShouldBeUpdated()
                .and().theDesignDocumentShouldNotExist()
                .given().theDesignDocumentUpdateRequestedMessage(designDocumentUpdateRequestedMessages.get(2))
                .when().publishTheMessage()
                .then().aDocumentUpdateCompletedMessageShouldBePublished()
                .and().theDocumentUpdateCompletedEventShouldHaveExpectedValues()
                .and().theDraftDesignDocumentShouldBeUpdated()
                .and().theDesignDocumentShouldNotExist()
                .given().theDesignDocumentUpdateRequestedMessage(designDocumentUpdateRequestedMessages.get(3))
                .when().publishTheMessage()
                .then().aDocumentUpdateCompletedMessageShouldBePublished()
                .and().theDocumentUpdateCompletedEventShouldHaveExpectedValues()
                .and().theDraftDesignDocumentShouldBeUpdated()
                .and().theDesignDocumentShouldBeUpdated();
    }

    public void shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequested(OutputMessage<DesignDocumentUpdateRequested> designDocumentUpdateRequestedMessage, OutputMessage<DesignDocumentDeleteRequested> designDocumentDeleteRequestedMessage) {
        getSteps()
                .given().theDesignDocumentUpdateRequestedMessage(designDocumentUpdateRequestedMessage)
                .when().publishTheMessage()
                .then().aDocumentUpdateCompletedMessageShouldBePublished()
                .and().theDocumentUpdateCompletedEventShouldHaveExpectedValues()
                .and().theDraftDesignDocumentShouldBeUpdated()
                .and().theDesignDocumentShouldBeUpdated()
                .given().theDesignDocumentDeleteRequestedMessage(designDocumentDeleteRequestedMessage)
                .when().publishTheMessage()
                .then().aDocumentDeleteCompletedMessageShouldBePublished()
                .and().theDocumentDeleteCompletedEventShouldHaveExpectedValues()
                .and().theDraftDesignDocumentShouldNotExist()
                .and().theDesignDocumentShouldNotExist();
    }

    private class TestActionsImpl implements TestActions {
        @Override
        public void clearMessages(Source source) {
            polling(source).clearMessages();
        }

        @Override
        public void emitMessage(Source source, OutputMessage<Object> message, Function<String, String> router) {
            emitter(source).send(message, router.apply(emitter(source).getTopicName()));
        }

        @Override
        public List<InputMessage<Object>> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage<Object>> messagePredicate) {
            return polling(source).findMessages(messageSource, messageType, keyPredicate, messagePredicate);
        }

        @Override
        public List<Design> findDesigns(UUID designId) {
            return vertx.getOrCreateContext()
                    .executeBlocking(() -> testElasticsearch.findDesigns(designId))
                    .onFailure(Throwable::printStackTrace)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join();
        }

        @Override
        public List<Design> findDraftDesigns(UUID designId) {
            return vertx.getOrCreateContext()
                    .executeBlocking(() -> testDraftElasticsearch.findDesigns(designId))
                    .onFailure(Throwable::printStackTrace)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join();
        }

        private KafkaTestPolling<Object, Payload> polling(Source source) {
            return switch (source) {
                case EVENTS -> eventsPolling;
            };
        }

        private KafkaTestEmitter<Object, Payload> emitter(Source source) {
            return switch (source) {
                case EVENTS -> eventEmitter;
            };
        }
    }
}
