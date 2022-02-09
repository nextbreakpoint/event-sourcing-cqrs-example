package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.vertx.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.designs.model.Design;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling eventsPolling;
    private KafkaTestEmitter eventEmitter;

    private TestElasticsearch testElasticsearch;

    private S3Client s3Client;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        TestS3.deleteContent(s3Client, TestConstants.BUCKET, object -> true);
        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
        TestS3.createBucket(s3Client, TestConstants.BUCKET);

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(vertx, createProducerConfig("integration"));

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig(consumerGroupId));

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);

        eventsPolling.startPolling();

        eventEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);

        final RestClient restClient = RestClient.builder(new HttpHost(scenario.getElasticsearchHost(), scenario.getElasticsearchPort())).build();

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());

        final ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));

        testElasticsearch = new TestElasticsearch(new ElasticsearchAsyncClient(transport), TestConstants.DESIGNS_INDEX_NAME);
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
    public URL makeBaseURL(String path) throws MalformedURLException {
        final String normPath = path.startsWith("/") ? path.substring(1) : path;
        return new URL("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
    }

    @NotNull
    public String makeAuthorization(String user, String role) {
        return scenario.makeAuthorization(user, role);
    }

    @NotNull
    public String getOriginUrl() {
        return "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort();
    }

    @NotNull
    public HttpsTestTarget getHttpsTestTarget() {
        return new HttpsTestTarget(scenario.getServiceHost(), scenario.getServicePort(), "/", true);
    }

    @NotNull
    public KafkaConsumerConfig createConsumerConfig(String groupId) {
        return KafkaConsumerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withAutoOffsetReset("earliest")
                .withEnableAutoCommit("false")
                .withGroupId(groupId)
                .build();
    }

    @NotNull
    public KafkaProducerConfig createProducerConfig(String clientId) {
        return KafkaProducerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withKeySerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withValueSerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withClientId(clientId)
                .withKafkaAcks("1")
                .build();
    }

    public void deleteDesigns() {
        testElasticsearch.deleteDesigns();
    }

    public void insertDesign(Design design) {
        testElasticsearch.insertDesign(design);

        final byte[] data = TestUtils.makeImage(256);

        TestUtils.generateKeys(design)
                .doOnNext(key -> TestS3.putObject(s3Client, TestConstants.BUCKET, key, data))
                .ignoreElements()
                .toCompletable()
                .await();
    }

    public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(List<OutputMessage> designDocumentUpdateRequestedMessages) {
        final DesignDocumentUpdateRequested designDocumentUpdateRequested1 = Json.decodeValue(designDocumentUpdateRequestedMessages.get(0).getValue().getData(), DesignDocumentUpdateRequested.class);
        final DesignDocumentUpdateRequested designDocumentUpdateRequested2 = Json.decodeValue(designDocumentUpdateRequestedMessages.get(1).getValue().getData(), DesignDocumentUpdateRequested.class);
        final DesignDocumentUpdateRequested designDocumentUpdateRequested3 = Json.decodeValue(designDocumentUpdateRequestedMessages.get(2).getValue().getData(), DesignDocumentUpdateRequested.class);
        final DesignDocumentUpdateRequested designDocumentUpdateRequested4 = Json.decodeValue(designDocumentUpdateRequestedMessages.get(3).getValue().getData(), DesignDocumentUpdateRequested.class);

        final UUID designId1 = designDocumentUpdateRequested1.getUuid();
        final UUID designId2 = designDocumentUpdateRequested4.getUuid();

        System.out.println("designId1 = " + designId1);
        System.out.println("designId2 = " + designId2);

        assertThat(designId1).isEqualTo(designDocumentUpdateRequested2.getUuid());
        assertThat(designId1).isEqualTo(designDocumentUpdateRequested3.getUuid());
        assertThat(designId2).isNotEqualTo(designDocumentUpdateRequested3.getUuid());

        eventsPolling.clearMessages();

        eventEmitter.send(designDocumentUpdateRequestedMessages.get(0));
        eventEmitter.send(designDocumentUpdateRequestedMessages.get(1));
        eventEmitter.send(designDocumentUpdateRequestedMessages.get(2));
        eventEmitter.send(designDocumentUpdateRequestedMessages.get(3));

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages1 = eventsPolling.findMessages(designId1.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
                    assertThat(messages1).hasSize(3);
                    TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(designId1, messages1.get(0), designDocumentUpdateRequested1.getJson(), designDocumentUpdateRequested1.getChecksum(), designDocumentUpdateRequested1.getStatus());
                    TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(designId1, messages1.get(1), designDocumentUpdateRequested2.getJson(), designDocumentUpdateRequested2.getChecksum(), designDocumentUpdateRequested2.getStatus());
                    TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(designId1, messages1.get(2), designDocumentUpdateRequested3.getJson(), designDocumentUpdateRequested3.getChecksum(), designDocumentUpdateRequested3.getStatus());
                    final List<InputMessage> messages2 = eventsPolling.findMessages(designId2.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
                    assertThat(messages2).hasSize(1);
                    TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(designId2, messages2.get(0), designDocumentUpdateRequested4.getJson(), designDocumentUpdateRequested4.getChecksum(), designDocumentUpdateRequested4.getStatus());
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages1 = eventsPolling.findMessages(designId1.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED);
                    assertThat(messages1).hasSize(3);
                    TestAssertions.assertExpectedDesignDocumentUpdateCompletedMessage(designId1, messages1.get(0));
                    TestAssertions.assertExpectedDesignDocumentUpdateCompletedMessage(designId1, messages1.get(1));
                    TestAssertions.assertExpectedDesignDocumentUpdateCompletedMessage(designId1, messages1.get(2));
                    final List<InputMessage> messages2 = eventsPolling.findMessages(designId2.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED);
                    assertThat(messages2).hasSize(1);
                    TestAssertions.assertExpectedDesignDocumentUpdateCompletedMessage(designId2, messages2.get(0));
                });

        await().atMost(Duration.of(20, SECONDS))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<Design> designs1 = testElasticsearch.findDesigns(designId1);
                    assertThat(designs1).hasSize(1);
                    TestAssertions.assertExpectedDesign(designs1.get(0), designId1, designDocumentUpdateRequested3.getJson(), designDocumentUpdateRequested3.getChecksum(), designDocumentUpdateRequested3.getStatus());
                    final List<Design> designs2 = testElasticsearch.findDesigns(designId2);
                    assertThat(designs2).hasSize(1);
                    TestAssertions.assertExpectedDesign(designs2.get(0), designId2, designDocumentUpdateRequested4.getJson(), designDocumentUpdateRequested4.getChecksum(), designDocumentUpdateRequested4.getStatus());
                });
    }

    public void shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage(OutputMessage designDocumentUpdateRequestedMessage, OutputMessage designDocumentDeleteRequestedMessage) {
        final DesignDocumentUpdateRequested designDocumentUpdateRequested = Json.decodeValue(designDocumentUpdateRequestedMessage.getValue().getData(), DesignDocumentUpdateRequested.class);
        final DesignDocumentDeleteRequested designDocumentDeleteRequested = Json.decodeValue(designDocumentDeleteRequestedMessage.getValue().getData(), DesignDocumentDeleteRequested.class);

        final UUID designId = designDocumentUpdateRequested.getUuid();

        System.out.println("designId1 = " + designId);

        assertThat(designId).isEqualTo(designDocumentUpdateRequested.getUuid());
        assertThat(designId).isEqualTo(designDocumentDeleteRequested.getUuid());

        eventsPolling.clearMessages();

        eventEmitter.send(designDocumentUpdateRequestedMessage);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignDocumentUpdateRequestedMessage(designId, messages.get(0), designDocumentUpdateRequested.getJson(), designDocumentUpdateRequested.getChecksum(), designDocumentUpdateRequested.getStatus());
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_UPDATE_COMPLETED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignDocumentUpdateCompletedMessage(designId, messages.get(0));
                });

        await().atMost(Duration.of(20, SECONDS))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<Design> designs = testElasticsearch.findDesigns(designId);
                    assertThat(designs).hasSize(1);
                    TestAssertions.assertExpectedDesign(designs.get(0), designId, designDocumentUpdateRequested.getJson(), designDocumentUpdateRequested.getChecksum(), designDocumentUpdateRequested.getStatus());
                });

        eventEmitter.send(designDocumentDeleteRequestedMessage);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignDocumentDeleteRequestedMessage(designId, messages.get(0));
                });

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.DESIGN_DOCUMENT_DELETE_COMPLETED);
                    assertThat(messages).hasSize(1);
                    TestAssertions.assertExpectedDesignDocumentDeleteCompletedMessage(designId, messages.get(0));
                });

        await().atMost(Duration.of(20, SECONDS))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<Design> designs = testElasticsearch.findDesigns(designId);
                    assertThat(designs).isEmpty();
                });
    }
}
