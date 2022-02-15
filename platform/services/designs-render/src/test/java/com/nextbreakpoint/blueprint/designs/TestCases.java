package com.nextbreakpoint.blueprint.designs;

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

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling eventsPolling;
    private KafkaTestPolling renderPolling;
    private KafkaTestEmitter eventsEmitter;
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

        eventsPolling = new KafkaTestPolling(eventsConsumer, TestConstants.EVENTS_TOPIC_NAME);
        renderPolling = new KafkaTestPolling(renderConsumer, TestConstants.RENDER_TOPIC_NAME);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        eventsEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);
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

        final TileRenderRequested tileRenderRequested1 = Json.decodeValue(tileRenderRequestedMessage1.getValue().getData(), TileRenderRequested.class);
        final TileRenderRequested tileRenderRequested2 = Json.decodeValue(tileRenderRequestedMessage2.getValue().getData(), TileRenderRequested.class);

        renderEmitter.send(tileRenderRequestedMessage1);
        renderEmitter.send(tileRenderRequestedMessage2);

        await().atMost(TEN_SECONDS)
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages1 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_1));
                    assertThat(messages1).hasSize(1);
                    final List<InputMessage> messages2 = renderPolling.findMessages(TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_2));
                    assertThat(messages2).hasSize(1);
                });

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(ONE_SECOND)
                .untilAsserted(() -> {
                    final List<InputMessage> messages = eventsPolling.findMessages(tileRenderRequested1.getDesignId().toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                    assertThat(messages).hasSize(2);
                    InputMessage message1 = messages.get(0);
                    InputMessage message2 = messages.get(1);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message1, tileRenderRequested1);
                    TestAssertions.assertExpectedTileRenderCompletedMessage(message2, tileRenderRequested2);
                });

        final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        ResponseBytes<GetObjectResponse> response1 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested1));
        assertThat(response1.asByteArray()).isNotEmpty();

        ResponseBytes<GetObjectResponse> response2 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested2));
        assertThat(response2.asByteArray()).isNotEmpty();
    }
}
