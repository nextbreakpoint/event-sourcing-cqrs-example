package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final TestContext context = new TestContext();

    private final TestSteps steps = new TestSteps(context, new TestActionsImpl());

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling renderPolling;
    private KafkaTestEmitter renderEmitter;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId + "-" + scenario.getUniqueTestId();
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(createProducerConfig("integration"));

        KafkaConsumer<String, String> renderConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        final Set<String> renderTopics = Set.of(
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-completed-0",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-completed-1",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-completed-2",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-completed-3",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-requested-0",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-requested-1",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-requested-2",
                TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId() + "-requested-3"
        );

        renderPolling = new KafkaTestPolling(renderConsumer, renderTopics);

        renderPolling.startPolling();

        renderEmitter = new KafkaTestEmitter(producer, TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId());

        deleteData();

        context.clear();
    }

    public void after() {
        if (renderPolling != null) {
            renderPolling.stopPolling();
        }

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

    public void deleteData() {
        final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));

        TestS3.deleteContent(s3Client, TestConstants.BUCKET, object -> true);
        TestS3.deleteBucket(s3Client, TestConstants.BUCKET);
        TestS3.createBucket(s3Client, TestConstants.BUCKET);
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
                .withKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer")
                .withAutoOffsetReset("earliest")
                .withEnableAutoCommit("false")
                .withGroupId(groupId)
                .build();
    }

    @NotNull
    private KafkaProducerConfig createProducerConfig(String clientId) {
        return KafkaProducerConfig.builder()
                .withBootstrapServers(scenario.getKafkaHost() + ":" + scenario.getKafkaPort())
                .withKeySerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withValueSerializer("org.apache.kafka.common.serialization.StringSerializer")
                .withClientId(clientId)
                .withKafkaAcks("1")
                .build();
    }

    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(List<OutputMessage> tileRenderRequestedMessages) {
        getSteps()
                .given().theTileRenderRequestedMessage(tileRenderRequestedMessages.get(0))
                .when().discardReceivedMessages(Source.RENDER)
                .and().publishTheMessage(Source.RENDER, topic -> topic +  "-requested-0")
                .then().aMessageShouldBePublished(Source.RENDER, TILE_RENDER_COMPLETED, key -> key.startsWith(DESIGN_ID_1.toString()))
                .and().theTileRenderCompletedMessageShouldHaveExpectedValues()
                .and().theTileRenderCompletedEventShouldHaveExpectedValues()
                .and().theImageShouldHasBeenCreated()
                .given().theTileRenderRequestedMessage(tileRenderRequestedMessages.get(1))
                .when().discardReceivedMessages(Source.RENDER)
                .and().publishTheMessage(Source.RENDER, topic -> topic +  "-requested-0")
                .then().aMessageShouldBePublished(Source.RENDER, TILE_RENDER_COMPLETED, key -> key.startsWith(DESIGN_ID_2.toString()))
                .and().theTileRenderCompletedMessageShouldHaveExpectedValues()
                .and().theTileRenderCompletedEventShouldHaveExpectedValues()
                .and().theImageShouldHasBeenCreated()
                .given().theTileRenderRequestedMessage(tileRenderRequestedMessages.get(2))
                .when().discardReceivedMessages(Source.RENDER)
                .and().publishTheMessage(Source.RENDER, topic -> topic +  "-requested-1")
                .then().aMessageShouldBePublished(Source.RENDER, TILE_RENDER_COMPLETED, key -> key.startsWith(DESIGN_ID_2.toString()))
                .and().theTileRenderCompletedMessageShouldHaveExpectedValues()
                .and().theTileRenderCompletedEventShouldHaveExpectedValues()
                .and().theImageShouldHasBeenCreated()
                .given().theTileRenderRequestedMessage(tileRenderRequestedMessages.get(3))
                .when().discardReceivedMessages(Source.RENDER)
                .and().publishTheMessage(Source.RENDER, topic -> topic +  "-requested-2")
                .then().aMessageShouldBePublished(Source.RENDER, TILE_RENDER_COMPLETED, key -> key.startsWith(DESIGN_ID_2.toString()))
                .and().theTileRenderCompletedMessageShouldHaveExpectedValues()
                .and().theTileRenderCompletedEventShouldHaveExpectedValues()
                .and().theImageShouldHasBeenCreated()
                .given().theTileRenderRequestedMessage(tileRenderRequestedMessages.get(4))
                .when().discardReceivedMessages(Source.RENDER)
                .and().publishTheMessage(Source.RENDER, topic -> topic +  "-requested-3")
                .then().aMessageShouldBePublished(Source.RENDER, TILE_RENDER_COMPLETED, key -> key.startsWith(DESIGN_ID_2.toString()))
                .and().theTileRenderCompletedMessageShouldHaveExpectedValues()
                .and().theTileRenderCompletedEventShouldHaveExpectedValues()
                .and().theImageShouldHasBeenCreated();
    }

    private class TestActionsImpl implements TestActions {
        @Override
        public void clearMessages(Source source) {
            polling(source).clearMessages();
        }

        @Override
        public void emitMessage(Source source, OutputMessage message, Function<String, String> router) {
            emitter(source).send(message, router.apply(emitter(source).getTopicName()));
        }

        @Override
        public List<InputMessage> findMessages(Source source, String messageSource, String messageType, Predicate<String> keyPredicate, Predicate<InputMessage> messagePredicate) {
            return polling(source).findMessages(messageSource, messageType, keyPredicate, messagePredicate);
        }

        @Override
        public byte[] getImage(String bucketKey) {
            final S3Client s3Client = TestS3.createS3Client(URI.create("http://" + scenario.getMinioHost() + ":" + scenario.getMinioPort()));
            ResponseBytes<GetObjectResponse> response = TestS3.getObject(s3Client, TestConstants.BUCKET, bucketKey);
            return response.asByteArray();
        }

        private KafkaTestPolling polling(Source source) {
            return switch (source) {
                case RENDER -> renderPolling;
            };
        }

        private KafkaTestEmitter emitter(Source source) {
            return switch (source) {
                case RENDER -> renderEmitter;
            };
        }
    }
}
