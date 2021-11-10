package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAbortRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.*;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;

public class IntegrationTests {
    private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String JSON_2 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 100] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";
    private static final String TILE_RENDER_REQUESTED = "tile-render-requested";
    private static final String TILE_RENDER_COMPLETED = "tile-render-completed";
    private static final String DESIGN_ABORT_REQUESTED = "design-abort-requested";
    private static final String MESSAGE_SOURCE = "service-designs";
    private static final String EVENTS_TOPIC_NAME = "design-event";
    private static final String RENDERING_QUEUE_TOPIC_NAME = "tiles-rendering-queue";
    private static final String BUCKET = "tiles";
    private static final String CHECKSUM_1 = Checksum.of(JSON_1);
    private static final String CHECKSUM_2 = Checksum.of(JSON_2);

    private static final TestScenario scenario = new TestScenario();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static final Environment environment = Environment.getDefaultEnvironment();

    private static KafkaTestPolling eventMessagesPolling;
    private static KafkaTestPolling renderMessagesPolling;
    private static KafkaTestEmitter renderMessageEmitter;

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

        eventMessagesConsumer.rxSubscribe(Collections.singleton(EVENTS_TOPIC_NAME))
//                .flatMap(ignore -> eventMessagesConsumer.rxPoll(Duration.ofSeconds(5)))
//                .flatMap(records -> eventMessagesConsumer.rxAssignment())
//                .flatMap(partitions -> eventMessagesConsumer.rxSeekToEnd(partitions))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();

        renderMessagesConsumer.rxSubscribe(Collections.singleton(RENDERING_QUEUE_TOPIC_NAME))
//                .flatMap(ignore -> renderMessagesConsumer.rxPoll(Duration.ofSeconds(5)))
//                .flatMap(records -> renderMessagesConsumer.rxAssignment())
//                .flatMap(partitions -> renderMessagesConsumer.rxSeekToEnd(partitions))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();

        eventMessagesPolling = new KafkaTestPolling(eventMessagesConsumer);
        renderMessagesPolling = new KafkaTestPolling(renderMessagesConsumer);

        eventMessagesPolling.startPolling();
        renderMessagesPolling.startPolling();

        renderMessageEmitter = new KafkaTestEmitter(producer, RENDERING_QUEUE_TOPIC_NAME);

        final S3Client s3Client = createS3Client();

        s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET).build())
                .stream()
                .forEach(response -> deleteObjects(s3Client, BUCKET, response.contents()));

//        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET).build());
//        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
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

            final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(Uuids.timeBased(), designId, 0, JSON_1, Checksum.of(JSON_1), 0, 0, 0);

            final OutputMessage tileRenderRequestedMessage1 = new TileRenderRequestedOutputMapper("test", IntegrationTests::createBucketKey).transform(tileRenderRequested1);

            renderMessageEmitter.sendMessage(tileRenderRequestedMessage1);

            final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(Uuids.timeBased(), designId, 1, JSON_2, Checksum.of(JSON_2), 1, 1, 2);

            final OutputMessage tileRenderRequestedMessage2 = new TileRenderRequestedOutputMapper("test", IntegrationTests::createBucketKey).transform(tileRenderRequested2);

            renderMessageEmitter.sendMessage(tileRenderRequestedMessage2);

            eventMessagesPolling.clearMessages();
            renderMessagesPolling.clearMessages();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages1 = renderMessagesPolling.findMessages("test", TILE_RENDER_REQUESTED, key -> key.startsWith(CHECKSUM_1));
                        assertThat(messages1).hasSize(1);
                        final List<InputMessage> messages2 = renderMessagesPolling.findMessages("test", TILE_RENDER_REQUESTED, key -> key.startsWith(CHECKSUM_2));
                        assertThat(messages2).hasSize(1);
                    });

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_COMPLETED);
                        assertThat(messages).hasSize(2);
                        InputMessage message1 = messages.get(0);
                        InputMessage message2 = messages.get(1);
                        assertExpectedTileRenderCompletedMessage(tileRenderRequested1, message1);
                        assertExpectedTileRenderCompletedMessage(tileRenderRequested2, message2);
                    });

            final S3Client s3Client = createS3Client();

            ResponseBytes<GetObjectResponse> response1 = getObject(s3Client, BUCKET, createBucketKey(tileRenderRequested1));
            assertThat(response1.asByteArray()).isNotEmpty();

            ResponseBytes<GetObjectResponse> response2 = getObject(s3Client, BUCKET, createBucketKey(tileRenderRequested2));
            assertThat(response2.asByteArray()).isNotEmpty();
        }

        @Test
        @Disabled
        @DisplayName("Should abort design after receiving a tile DesignAbortRequested event")
        public void shouldAbortRenderingWhenReceivingAMessage() {
            final UUID designId = UUID.fromString("ea55b659-a6df-409c-9c5b-85ea067f0f38");

            System.out.println("designId = " + designId);

            final DesignAbortRequested designAbortRequested1 = new DesignAbortRequested(Uuids.timeBased(), designId, Checksum.of(JSON_1));

            final OutputMessage designAbortRequestedMessage1 = new DesignAbortRequestedOutputMapper("test").transform(designAbortRequested1);

            renderMessageEmitter.sendMessage(designAbortRequestedMessage1);

//            final DesignAbortRequested designAbortRequested2 = new DesignAbortRequested(designId, System.currentTimeMillis(), Checksum.of(JSON_2));
//
//            final OutputMessage designAbortRequestedMessage2 = new DesignAbortRequestedOutputMapper("test").transform(designAbortRequested2);
//
//            renderMessageEmitter.sendMessage(designAbortRequestedMessage2);

            eventMessagesPolling.clearMessages();
            renderMessagesPolling.clearMessages();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<InputMessage> messages = eventMessagesPolling.findMessages(designId.toString(), "test", DESIGN_ABORT_REQUESTED);
                        assertThat(messages).hasSize(1);
                    });
        }
    }

    private void assertExpectedTileRenderCompletedMessage(TileRenderRequested tileRenderRequested, InputMessage actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getValue().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(tileRenderRequested.getUuid().toString());
        assertThat(actualMessage.getValue().getUuid()).isNotNull();
        assertThat(actualMessage.getValue().getType()).isEqualTo(TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getValue()).isNotNull();
        TileRenderCompleted actualEvent = Json.decodeValue(actualMessage.getValue().getData(), TileRenderCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(tileRenderRequested.getUuid());
        assertThat(actualEvent.getLevel()).isEqualTo(tileRenderRequested.getLevel());
        assertThat(actualEvent.getRow()).isEqualTo(tileRenderRequested.getRow());
        assertThat(actualEvent.getCol()).isEqualTo(tileRenderRequested.getCol());
    }

    private static S3Client createS3Client() {
        return S3Client.builder()
                .region(Region.EU_WEST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("admin", "password")))
                .endpointOverride(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()))
                .build();
    }

    private static void deleteObjects(S3Client s3Client, String bucket, List<S3Object> objects) {
        objects.forEach(object -> deleteObject(s3Client, bucket, object.key()));
    }

    private static DeleteObjectResponse deleteObject(S3Client s3Client, String bucket, String key) {
        return s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private static ResponseBytes<GetObjectResponse> getObject(S3Client s3Client, String bucket, String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private static String createBucketKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getChecksum(), event.getLevel(), event.getRow(), event.getCol());
    }
}
