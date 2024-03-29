package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.drivers.CassandraClientConfig;
import com.nextbreakpoint.blueprint.common.drivers.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.test.KafkaTestEmitter;
import com.nextbreakpoint.blueprint.common.test.KafkaTestPolling;
import com.nextbreakpoint.blueprint.common.test.TestContext;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.designs.TestActions.Source;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_AGGREGATE_UPDATED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;

public class TestCases {
    private final TestScenario scenario = new TestScenario();

    private final TestContext context = new TestContext();

    private final TestSteps steps = new TestSteps(context, new TestActionsImpl());

    private final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    private KafkaTestPolling<Object, Payload> eventsPolling;
    private KafkaTestPolling<Object, Payload> renderPolling;
    private KafkaTestEmitter<Object, Payload> eventEmitter;
    private KafkaTestEmitter<Object, Payload> renderEmitter;

    private TestCassandra testCassandra;

    private String consumerGroupId;

    public TestCases(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId + "-" + scenario.getUniqueTestId();
    }

    public void before() {
        scenario.before();

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        testCassandra = new TestCassandra(CassandraClientFactory.create(createCassandraConfig()));

        KafkaProducer<String, Payload> producer = KafkaClientFactory.createProducer(createProducerConfig("integration"));

        KafkaConsumer<String, Payload> eventsConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

        KafkaConsumer<String, Payload> renderConsumer = KafkaClientFactory.createConsumer(createConsumerConfig(consumerGroupId));

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

        eventsPolling = new KafkaTestPolling<>(eventsConsumer, Records.createEventInputRecordMapper(), TestConstants.EVENTS_TOPIC_NAME + "-" + scenario.getUniqueTestId());
        renderPolling = new KafkaTestPolling<>(renderConsumer, Records.createEventInputRecordMapper(), renderTopics);

        eventsPolling.startPolling();
        renderPolling.startPolling();

        eventEmitter = new KafkaTestEmitter<>(producer, Records.createEventOutputRecordMapper(), TestConstants.EVENTS_TOPIC_NAME + "-" + scenario.getUniqueTestId());
        renderEmitter = new KafkaTestEmitter<>(producer, Records.createEventOutputRecordMapper(), TestConstants.RENDER_TOPIC_PREFIX + "-" + scenario.getUniqueTestId());

        deleteData();

        context.clear();
    }

    public void after() {
        if (eventsPolling != null) {
            eventsPolling.stopPolling();
        }
        if (eventsPolling != null) {
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
        testCassandra.deleteMessages();
        testCassandra.deleteDesigns();
    }

    @NotNull
    private CassandraClientConfig createCassandraConfig() {
        return CassandraClientConfig.builder()
                .withClusterName("datacenter1")
                .withKeyspace(TestConstants.DATABASE_KEYSPACE)
                .withUsername("admin")
                .withPassword("password")
                .withContactPoints(new String[] { scenario.getCassandraHost() })
                .withPort(scenario.getCassandraPort())
                .build();
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

    public void shouldUpdateTheDesignAfterReceivingADesignInsertRequestedMessage(OutputMessage<DesignInsertRequested> designInsertRequestedMessage) {
        getSteps()
                .given().theDesignInsertRequestedMessage(designInsertRequestedMessage)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().theMessageShouldBeSaved(DesignInsertRequested.class)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().theAggregateUpdatedMessageHasExpectedValues("CREATED", LEVELS_DRAFT)
                .and().manyMessagesShouldBePublished(Source.RENDER, TILE_RENDER_REQUESTED, key -> key.startsWith(DESIGN_ID_1.toString()), TestUtils.totalTilesByLevels(LEVELS_DRAFT))
                .and().theTileRenderRequestedMessagesHaveExpectedValues(LEVELS_DRAFT)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .and().theDesignDocumentUpdateRequestedMessageHasExpectedValues("CREATED", LEVELS_DRAFT)
                .and().theDesignShouldBeSaved("CREATED", LEVELS_DRAFT);
    }

    public void shouldUpdateTheDesignAfterReceivingADesignUpdateRequestedMessage(OutputMessage<DesignInsertRequested> designInsertRequestedMessage, OutputMessage<DesignUpdateRequested> designUpdateRequestedMessage1, OutputMessage<DesignUpdateRequested> designUpdateRequestedMessage2) {
        getSteps()
                .given().theDesignInsertRequestedMessage(designInsertRequestedMessage)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().manyMessagesShouldBePublished(Source.RENDER, TILE_RENDER_REQUESTED, key -> key.startsWith(DESIGN_ID_2.toString()), TestUtils.totalTilesByLevels(LEVELS_DRAFT))
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .given().theDesignUpdateRequestedMessage(designUpdateRequestedMessage1)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().theMessageShouldBeSaved(DesignUpdateRequested.class)
                .and().theDesignShouldBeSaved("UPDATED", LEVELS_DRAFT)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().theAggregateUpdatedMessageHasExpectedValues("UPDATED", LEVELS_DRAFT)
                .and().manyMessagesShouldBePublished(Source.RENDER, TILE_RENDER_REQUESTED, key -> key.startsWith(DESIGN_ID_2.toString()), TestUtils.totalTilesByLevels(LEVELS_DRAFT))
                .and().theTileRenderRequestedMessagesHaveExpectedValues(LEVELS_DRAFT)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .and().theDesignDocumentUpdateRequestedMessageHasExpectedValues("UPDATED", LEVELS_DRAFT)
                .given().theDesignUpdateRequestedMessage(designUpdateRequestedMessage2)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().theMessageShouldBeSaved(DesignUpdateRequested.class)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().theAggregateUpdatedMessageHasExpectedValues("UPDATED", LEVELS_READY)
                .and().manyMessagesShouldBePublished(Source.RENDER, TILE_RENDER_REQUESTED, key -> key.startsWith(DESIGN_ID_2.toString()), TestUtils.totalTilesByLevels(LEVELS_DRAFT))
                .and().theTileRenderRequestedMessagesHaveExpectedValues(LEVELS_DRAFT)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .and().theDesignDocumentUpdateRequestedMessageHasExpectedValues("UPDATED", LEVELS_READY)
                .and().theDesignShouldBeSaved("UPDATED", LEVELS_READY);
    }

    public void shouldUpdateTheDesignAfterReceivingADesignDeleteRequestedMessage(OutputMessage<DesignInsertRequested> designInsertRequestedMessage, OutputMessage<DesignDeleteRequested> designDeleteRequestedMessage) {
        getSteps()
                .given().theDesignInsertRequestedMessage(designInsertRequestedMessage)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().manyMessagesShouldBePublished(Source.RENDER, TILE_RENDER_REQUESTED, key -> key.startsWith(DESIGN_ID_3.toString()), TestUtils.totalTilesByLevels(LEVELS_DRAFT))
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .given().theDesignDeleteRequestedMessage(designDeleteRequestedMessage)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().theMessageShouldBeSaved(DesignDeleteRequested.class)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().theAggregateUpdatedMessageHasExpectedValues("DELETED", LEVELS_DRAFT)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_DELETE_REQUESTED)
                .and().theDesignDocumentDeleteRequestedMessageHasExpectedValues()
                .and().theDesignShouldBeSaved("DELETED", LEVELS_DRAFT);
    }

    public void shouldUpdateTheDesignAfterReceivingATileRenderCompletedMessage(OutputMessage<DesignInsertRequested> designInsertRequestedMessage, List<OutputMessage<TileRenderCompleted>> tileRenderCompletedMessages, Bitmap bitmap) {
        getSteps()
                .given().theDesignInsertRequestedMessage(designInsertRequestedMessage)
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.EVENTS)
                .then().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().manyMessagesShouldBePublished(Source.RENDER, TILE_RENDER_REQUESTED, key -> key.startsWith(DESIGN_ID_2.toString()), TestUtils.totalTilesByLevels(LEVELS_DRAFT))
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .given().theTileRenderCompletedMessage(tileRenderCompletedMessages.get(0))
                .when().discardReceivedMessages(Source.RENDER)
                .and().discardReceivedMessages(Source.EVENTS)
                .and().publishTheMessage(Source.RENDER, topic -> topic + "-completed-0")
                .given().theTileRenderCompletedMessage(tileRenderCompletedMessages.get(1))
                .when().publishTheMessage(Source.RENDER, topic -> topic + "-completed-1")
                .given().theTileRenderCompletedMessage(tileRenderCompletedMessages.get(2))
                .when().publishTheMessage(Source.RENDER, topic -> topic + "-completed-1")
                .given().theTileRenderCompletedMessage(tileRenderCompletedMessages.get(3))
                .when().publishTheMessage(Source.RENDER, topic -> topic + "-completed-2")
                .given().theTileRenderCompletedMessage(tileRenderCompletedMessages.get(4))
                .when().publishTheMessage(Source.RENDER, topic -> topic + "-completed-2")
                .and().selectDesignId(DESIGN_ID_2)
                .then().aMessageShouldBePublished(Source.EVENTS, DESIGN_AGGREGATE_UPDATED)
                .and().theAggregateUpdatedMessageHasExpectedValues("CREATED", LEVELS_DRAFT)
                .and().aMessageShouldBePublished(Source.EVENTS, DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .and().theDesignDocumentUpdateRequestedMessageHasExpectedValues("CREATED", LEVELS_DRAFT)
                .and().theDesignShouldBeSaved("CREATED", bitmap, LEVELS_DRAFT);
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
        public List<Row> fetchMessages(UUID designId, UUID messageId) {
            return testCassandra.fetchMessages(designId, messageId);
        }

        @Override
        public List<Row> fetchDesigns(UUID designId) {
            return testCassandra.fetchDesigns(designId);
        }

        private KafkaTestPolling<Object, Payload> polling(Source source) {
            return switch (source) {
                case EVENTS -> eventsPolling;
                case RENDER -> renderPolling;
            };
        }

        private KafkaTestEmitter<Object, Payload> emitter(Source source) {
            return switch (source) {
                case EVENTS -> eventEmitter;
                case RENDER -> renderEmitter;
            };
        }
    }
}
