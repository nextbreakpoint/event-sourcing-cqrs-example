package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAbortRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.*;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class IntegrationTests {
    private static final TestScenario scenario = new TestScenario();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static final Environment environment = Environment.getDefaultEnvironment();

    private static KafkaTestPolling eventsPolling;
    private static KafkaTestPolling renderPolling;
    private static KafkaTestEmitter eventsEmitter;
    private static KafkaTestEmitter renderEmitter;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        KafkaConsumer<String, String> eventMessagesConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        KafkaConsumer<String, String> renderMessagesConsumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test-rendering"));

        eventsPolling = new KafkaTestPolling(eventMessagesConsumer, TestConstants.EVENTS_TOPIC_NAME);
        renderPolling = new KafkaTestPolling(renderMessagesConsumer, TestConstants.RENDERING_QUEUE_TOPIC_NAME);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        eventsEmitter = new KafkaTestEmitter(producer, TestConstants.EVENTS_TOPIC_NAME);
        renderEmitter = new KafkaTestEmitter(producer, TestConstants.RENDERING_QUEUE_TOPIC_NAME);

        final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

//        TestS3.deleteContent(s3Client, TestConstants.BUCKET);
//        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
//        TestS3.createBucket(s3Client, TestConstants.BUCKET);
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        try {
            vertx.rxClose()
                    .subscribeOn(Schedulers.computation())
                    .doOnError(Throwable::printStackTrace)
                    .toBlocking()
                    .value();
        } catch (Exception ignore) {
        }

        scenario.after();
    }

    @Nested
    @Tag("slow")
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-tile-renderer service")
    public class VerifyServiceApi {
        @AfterEach
        public void reset() {
            RestAssured.reset();
        }

        @Test
        @DisplayName("Should render the image after receiving a tile TileRenderRequested event")
        public void shouldRenderImageWhenReceivingAMessage() {
            final UUID designId = UUID.randomUUID();

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper("test").transform(designInsertRequested);

            eventsEmitter.sendMessage(designInsertRequestedMessage);

            final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(Uuids.timeBased(), designId, 0, TestConstants.JSON_1, Checksum.of(TestConstants.JSON_1), 0, 0, 0);

            final OutputMessage tileRenderRequestedMessage1 = new TileRenderRequestedOutputMapper("test", TestUtils::createBucketKey).transform(tileRenderRequested1);

            renderEmitter.sendMessage(tileRenderRequestedMessage1);

            final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(Uuids.timeBased(), designId, 1, TestConstants.JSON_2, Checksum.of(TestConstants.JSON_2), 1, 1, 2);

            final OutputMessage tileRenderRequestedMessage2 = new TileRenderRequestedOutputMapper("test", TestUtils::createBucketKey).transform(tileRenderRequested2);

            renderEmitter.sendMessage(tileRenderRequestedMessage2);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_INSERT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = renderPolling.findMessages("test", TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_1));
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = renderPolling.findMessages("test", TestConstants.TILE_RENDER_REQUESTED, key -> key.startsWith(TestConstants.CHECKSUM_2));
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), TestConstants.MESSAGE_SOURCE, TestConstants.TILE_RENDER_COMPLETED);
                        assertThat(messages).hasSize(2);
                        InputMessage message1 = messages.get(0);
                        InputMessage message2 = messages.get(1);
                        TestAssertions.assertExpectedTileRenderCompletedMessage(tileRenderRequested1, message1);
                        TestAssertions.assertExpectedTileRenderCompletedMessage(tileRenderRequested2, message2);
                    });

            final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

            ResponseBytes<GetObjectResponse> response1 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested1));
            assertThat(response1.asByteArray()).isNotEmpty();

            ResponseBytes<GetObjectResponse> response2 = TestS3.getObject(s3Client, TestConstants.BUCKET, TestUtils.createBucketKey(tileRenderRequested2));
            assertThat(response2.asByteArray()).isNotEmpty();
        }

        @Test
        @Disabled
        @DisplayName("Should abort design after receiving a tile DesignAbortRequested event")
        public void shouldAbortRenderingWhenReceivingAMessage() {
            final UUID designId = UUID.fromString("ea55b659-a6df-409c-9c5b-85ea067f0f38");

            System.out.println("designId = " + designId);

            eventsPolling.clearMessages();
            renderPolling.clearMessages();

            final DesignAbortRequested designAbortRequested1 = new DesignAbortRequested(Uuids.timeBased(), designId, Checksum.of(TestConstants.JSON_1));

            final OutputMessage designAbortRequestedMessage1 = new DesignAbortRequestedOutputMapper("test").transform(designAbortRequested1);

            renderEmitter.sendMessage(designAbortRequestedMessage1);

//            final DesignAbortRequested designAbortRequested2 = new DesignAbortRequested(designId, System.currentTimeMillis(), Checksum.of(TestConstants.JSON_2));
//
//            final OutputMessage designAbortRequestedMessage2 = new DesignAbortRequestedOutputMapper("test").transform(designAbortRequested2);
//
//            renderMessageEmitter.sendMessage(designAbortRequestedMessage2);

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventsPolling.findMessages(designId.toString(), "test", TestConstants.DESIGN_ABORT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });
        }
    }
}