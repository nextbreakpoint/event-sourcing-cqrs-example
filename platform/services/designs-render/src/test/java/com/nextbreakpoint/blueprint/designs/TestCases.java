package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.vertx.KafkaProducerConfig;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling eventsPolling;
    private KafkaTestPolling renderPolling;
    private KafkaTestEmitter renderEmitter;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(vertx, createProducerConfig("integration"));

        KafkaConsumer<String, String> eventsConsumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig(consumerGroupId));

        KafkaConsumer<String, String> renderConsumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig(consumerGroupId));

        final Set<String> renderTopics = Set.of(
                TestConstants.RENDER_TOPIC_NAME + "-0",
                TestConstants.RENDER_TOPIC_NAME + "-1",
                TestConstants.RENDER_TOPIC_NAME + "-2",
                TestConstants.RENDER_TOPIC_NAME + "-3",
                TestConstants.RENDER_TOPIC_NAME + "-4"
        );

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);
        renderPolling = new KafkaTestPolling(renderConsumer, renderTopics);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        renderEmitter = new KafkaTestEmitter(producer, TestConstants.RENDER_TOPIC_NAME);

        final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        TestS3.deleteContent(s3Client, TestConstants.BUCKET, object -> true);
        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
        TestS3.createBucket(s3Client, TestConstants.BUCKET);
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

    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(List<OutputMessage> tileRenderRequestedMessages) {
        eventsPolling.clearMessages();
        renderPolling.clearMessages();

        final OutputMessage tileRenderRequestedMessage1 = tileRenderRequestedMessages.get(0);
        final OutputMessage tileRenderRequestedMessage2 = tileRenderRequestedMessages.get(1);
        final OutputMessage tileRenderRequestedMessage3 = tileRenderRequestedMessages.get(2);
        final OutputMessage tileRenderRequestedMessage4 = tileRenderRequestedMessages.get(3);
        final OutputMessage tileRenderRequestedMessage5 = tileRenderRequestedMessages.get(4);

        final TileRenderRequested tileRenderRequested1 = Json.decodeValue(tileRenderRequestedMessage1.getValue().getData(), TileRenderRequested.class);
        final TileRenderRequested tileRenderRequested2 = Json.decodeValue(tileRenderRequestedMessage2.getValue().getData(), TileRenderRequested.class);
        final TileRenderRequested tileRenderRequested3 = Json.decodeValue(tileRenderRequestedMessage2.getValue().getData(), TileRenderRequested.class);
        final TileRenderRequested tileRenderRequested4 = Json.decodeValue(tileRenderRequestedMessage2.getValue().getData(), TileRenderRequested.class);
        final TileRenderRequested tileRenderRequested5 = Json.decodeValue(tileRenderRequestedMessage2.getValue().getData(), TileRenderRequested.class);

        renderEmitter.send(tileRenderRequestedMessage1, renderEmitter.getTopicName() + "-0");
        renderEmitter.send(tileRenderRequestedMessage2, renderEmitter.getTopicName() + "-1");
        renderEmitter.send(tileRenderRequestedMessage3, renderEmitter.getTopicName() + "-2");
        renderEmitter.send(tileRenderRequestedMessage4, renderEmitter.getTopicName() + "-3");
        renderEmitter.send(tileRenderRequestedMessage5, renderEmitter.getTopicName() + "-4");

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages1 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(tileRenderRequested1.getDesignId().toString()));
                    assertThat(messages1).hasSize(1);
                    final List<InputMessage> messages2 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(tileRenderRequested2.getDesignId().toString()));
                    assertThat(messages2).hasSize(1);
                    final List<InputMessage> messages3 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(tileRenderRequested3.getDesignId().toString()));
                    assertThat(messages3).hasSize(1);
                    final List<InputMessage> messages4 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(tileRenderRequested4.getDesignId().toString()));
                    assertThat(messages4).hasSize(1);
                    final List<InputMessage> messages5 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(tileRenderRequested5.getDesignId().toString()));
                    assertThat(messages5).hasSize(1);
                });

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages1 = eventsPolling.findMessages(tileRenderRequested1.getDesignId().toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                    assertThat(messages1).hasSize(1);
                    final List<InputMessage> messages2 = eventsPolling.findMessages(tileRenderRequested2.getDesignId().toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                    assertThat(messages2).hasSize(1);
                    final List<InputMessage> messages3 = eventsPolling.findMessages(tileRenderRequested3.getDesignId().toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                    assertThat(messages3).hasSize(1);
                    final List<InputMessage> messages4 = eventsPolling.findMessages(tileRenderRequested4.getDesignId().toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                    assertThat(messages4).hasSize(1);
                    final List<InputMessage> messages5 = eventsPolling.findMessages(tileRenderRequested5.getDesignId().toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                    assertThat(messages5).hasSize(1);
                    InputMessage message1 = messages1.get(0);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message1, tileRenderRequested1);
                    InputMessage message2 = messages2.get(0);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message2, tileRenderRequested2);
                    InputMessage message3 = messages3.get(0);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message3, tileRenderRequested3);
                    InputMessage message4 = messages4.get(0);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message4, tileRenderRequested4);
                    InputMessage message5 = messages5.get(0);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message5, tileRenderRequested5);
                });

        final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        ResponseBytes<GetObjectResponse> response1 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested1));
        assertThat(response1.asByteArray()).isNotEmpty();

        ResponseBytes<GetObjectResponse> response2 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested2));
        assertThat(response2.asByteArray()).isNotEmpty();

        ResponseBytes<GetObjectResponse> response3 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested3));
        assertThat(response3.asByteArray()).isNotEmpty();

        ResponseBytes<GetObjectResponse> response4 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested4));
        assertThat(response4.asByteArray()).isNotEmpty();

        ResponseBytes<GetObjectResponse> response5 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested5));
        assertThat(response5.asByteArray()).isNotEmpty();
    }
}
