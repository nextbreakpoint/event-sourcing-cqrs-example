package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.KafkaClientFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducerRecord;
import org.jetbrains.annotations.NotNull;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private static final TestScenario scenario = new TestScenario();

    private static final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private static final Environment environment = Environment.getDefaultEnvironment();

    private static final List<KafkaConsumerRecord<String, String>> records = new ArrayList<>();

    private static KafkaConsumer<String, String> consumer;
    private static KafkaProducer<String, String> producer;

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
        RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

        final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

        producer = KafkaClientFactory.createProducer(environment, vertx, scenario.createProducerConfig());

        consumer = KafkaClientFactory.createConsumer(environment, vertx, scenario.createConsumerConfig("test"));

        consumer.rxSubscribe(Collections.singleton(EVENTS_TOPIC_NAME))
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .value();

        pollRecords();

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

            final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(Uuids.timeBased(), designId, 0, JSON_1, Checksum.of(JSON_1), 0, 0, 0);

            final Message tileRenderRequestedMessage1 = createTileRenderRequestedMessage(UUID.randomUUID(), createBucketKey(tileRenderRequested1), System.currentTimeMillis(), tileRenderRequested1);

            producer.rxSend(createKafkaRecord(tileRenderRequestedMessage1))
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toBlocking()
                    .value();

            final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(Uuids.timeBased(), designId, 1, JSON_2, Checksum.of(JSON_2), 1, 1, 2);

            final Message tileRenderRequestedMessage2 = createTileRenderRequestedMessage(UUID.randomUUID(), createBucketKey(tileRenderRequested1), System.currentTimeMillis(), tileRenderRequested2);

            producer.rxSend(createKafkaRecord(tileRenderRequestedMessage2))
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toBlocking()
                    .value();

            safelyClearMessages();

            await().atMost(TEN_SECONDS)
                    .pollInterval(ONE_SECOND)
                    .untilAsserted(() -> {
                        final List<Message> messages = safelyFindMessages(designId.toString(), MESSAGE_SOURCE, TILE_RENDER_COMPLETED);
                        assertThat(messages).hasSize(2);
                        Message message1 = messages.get(0);
                        Message message2 = messages.get(1);
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

            final DesignAbortRequested designAbortRequested1 = new DesignAbortRequested(Uuids.timeBased(), designId, Checksum.of(JSON_1));

            final Message designAbortRequestedMessage1 = createDesignAbortRequestedMessage(UUID.randomUUID(), designId.toString(), System.currentTimeMillis(), designAbortRequested1);

            producer.rxSend(createKafkaRecord(designAbortRequestedMessage1))
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.io())
                    .toBlocking()
                    .value();

//            final DesignAbortRequested designAbortRequested2 = new DesignAbortRequested(designId, System.currentTimeMillis(), Checksum.of(JSON_2));
//
//            final Message designAbortRequestedMessage2 = createDesignAbortRequestedMessage(UUID.randomUUID(), designId.toString(), System.currentTimeMillis(), designAbortRequested2);
//
//            producer.rxSend(createKafkaRecord(designAbortRequestedMessage2))
//                    .subscribeOn(Schedulers.computation())
//                    .doOnError(Throwable::printStackTrace)
//                    .toBlocking()
//                    .value();

            safelyClearMessages();
        }
    }

    private void assertExpectedTileRenderCompletedMessage(TileRenderRequested tileRenderRequested, Message actualMessage) {
        assertThat(actualMessage.getTimestamp()).isNotNull();
        assertThat(actualMessage.getPayload().getSource()).isEqualTo(MESSAGE_SOURCE);
        assertThat(actualMessage.getKey()).isEqualTo(tileRenderRequested.getUuid().toString());
        assertThat(actualMessage.getPayload().getUuid()).isNotNull();
        assertThat(actualMessage.getPayload().getType()).isEqualTo(TILE_RENDER_COMPLETED);
        assertThat(actualMessage.getPayload()).isNotNull();
        TileRenderCompleted actualEvent = Json.decodeValue(actualMessage.getPayload().getData(), TileRenderCompleted.class);
        assertThat(actualEvent.getUuid()).isEqualTo(tileRenderRequested.getUuid());
        assertThat(actualEvent.getLevel()).isEqualTo(tileRenderRequested.getLevel());
        assertThat(actualEvent.getRow()).isEqualTo(tileRenderRequested.getRow());
        assertThat(actualEvent.getCol()).isEqualTo(tileRenderRequested.getCol());
    }

    @NotNull
    private static KafkaProducerRecord<String, String> createKafkaRecord(Message message) {
        return KafkaProducerRecord.create(RENDERING_QUEUE_TOPIC_NAME, message.getKey(), Json.encode(message.getPayload()));
    }

    private static Message createTileRenderRequestedMessage(UUID messageId, String partitionKey, long timestamp, TileRenderRequested event) {
        return new Message(partitionKey, 0, timestamp,  new Payload(messageId, TILE_RENDER_REQUESTED, Json.encode(event), "test"));
    }

    private static Message createDesignAbortRequestedMessage(UUID messageId, String partitionKey, long timestamp, DesignAbortRequested event) {
        return new Message(partitionKey, 0, timestamp,  new Payload(messageId, DESIGN_ABORT_REQUESTED, Json.encode(event), "test"));
    }

    private static void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @NotNull
    private static List<Message> safelyFindMessages(String partitionKey, String messageSource, String messageType) {
        synchronized (records) {
            return records.stream()
                    .map(record -> new Message(record.key(), record.offset(), record.timestamp(), Json.decodeValue(record.value(), Payload.class)))
                    .filter(message -> message.getKey().equals(partitionKey))
                    .filter(message -> message.getPayload().getSource().equals(messageSource))
                    .filter(message -> message.getPayload().getType().equals(messageType))
//                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    private static void safelyClearMessages() {
        synchronized (records) {
            records.clear();
        }
    }

    private static void safelyAppendRecord(KafkaConsumerRecord<String, String> record) {
        synchronized (records) {
            records.add(record);
        }
    }

    private static void consumeRecords(KafkaConsumerRecords<String, String> consumerRecords) {
//        System.out.println("Received " + consumerRecords.size() + " messages");

        IntStream.range(0, consumerRecords.size())
                .forEach(index -> safelyAppendRecord(consumerRecords.recordAt(index)));

        pollRecords();
        commitOffsets();
    }

    private static void pollRecords() {
        consumer.rxPoll(Duration.ofSeconds(5))
                .doOnSuccess(IntegrationTests::consumeRecords)
                .subscribeOn(Schedulers.io())
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    private static void commitOffsets() {
        consumer.rxCommit()
                .doOnError(Throwable::printStackTrace)
                .subscribeOn(Schedulers.io())
                .subscribe();
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
