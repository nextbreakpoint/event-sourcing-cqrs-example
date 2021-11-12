package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.AmpqTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.*;

public class PactTests {
//    private static final String UUID_REGEXP = "[0-9a-f]{8}-[0-9a-f]{4}-[5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

//    private static final UUID DESIGN_UUID_1 = new UUID(0L, 1L);
//    private static final UUID DESIGN_UUID_2 = new UUID(0L, 2L);
//    private static final UUID DESIGN_UUID_3 = new UUID(0L, 3L);
//    private static final UUID DESIGN_UUID_4 = new UUID(0L, 4L);

    private static TestCases testCases = new TestCases("PactTests");

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        testCases.before();

        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", testCases.getVersion());
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        testCases.after();
    }

    @Nested
    @Tag("slow")
    @Tag("pact")
    @DisplayName("Test designs-event-consumer pact")
    @ExtendWith(PactConsumerTestExt.class)
    public class TestDesignsCommandConsumer {
        @Pact(consumer = "designs-event-consumer")
        public MessagePact designInsertRequested(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact designUpdateRequested(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload1 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value1 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload1.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value1.toString());

            PactDslJsonBody payload2 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_2)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.DESIGN_UPDATE_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value2.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("command to update design")
                    .withContent(message2)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact designDeleteRequested(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload1 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value1 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload1.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value1.toString());

            PactDslJsonBody payload2 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased());

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.DESIGN_DELETE_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value2.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("command to delete design")
                    .withContent(message2)
                    .toPact();
        }

        @Pact(consumer = "designs-event-consumer")
        public MessagePact tileRenderCompleted(MessagePactBuilder builder) {
            UUID uuid = UUID.randomUUID();

            PactDslJsonBody payload1 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .stringValue("data", TestConstants.JSON_1)
                    .numberValue("levels", TestConstants.LEVELS);

            PactDslJsonBody value1 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload1.toString())
                    .stringValue("type", TestConstants.DESIGN_INSERT_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message1 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value1.toString());

            PactDslJsonBody payload2 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .numberValue("esid", 0)
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 0)
                    .numberValue("row", 0)
                    .numberValue("col", 0)
                    .stringValue("status", "FAILED");

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value2.toString());

            PactDslJsonBody payload3 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .numberValue("esid", 0)
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 0)
                    .numberValue("col", 0)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value3 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload3.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message3 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value3.toString());

            PactDslJsonBody payload4 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .numberValue("esid", 0)
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 1)
                    .numberValue("col", 0)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value4 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload4.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message4 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value4.toString());

            PactDslJsonBody payload5 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .numberValue("esid", 0)
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 2)
                    .numberValue("col", 1)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value5 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload5.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message5 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value5.toString());

            PactDslJsonBody payload6 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .numberValue("esid", 0)
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 1)
                    .numberValue("row", 3)
                    .numberValue("col", 1)
                    .stringValue("status", "COMPLETED");

            PactDslJsonBody value6 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload6.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_COMPLETED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message6 = new PactDslJsonBody()
                    .stringValue("key", uuid.toString())
                    .stringValue("value", value6.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("command to insert design")
                    .withContent(message1)
                    .expectsToReceive("tile render completed")
                    .withContent(message2)
                    .expectsToReceive("tile render completed")
                    .withContent(message3)
                    .expectsToReceive("tile render completed")
                    .withContent(message4)
                    .expectsToReceive("tile render completed")
                    .withContent(message5)
                    .expectsToReceive("tile render completed")
                    .withContent(message6)
                    .toPact();
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1111", pactMethod = "designInsertRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a DesignInsertRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord.getKey(), Json.decodeValue(kafkaRecord.getValue(), Payload.class));

            testCases.shouldUpdateTheDesignWhenReceivingADesignInsertRequestedMessage(List.of(designInsertRequestedMessage).get(0));
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1112", pactMethod = "designUpdateRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a DesignUpdateRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage designUpdateRequestedMessage = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            testCases.shouldUpdateTheDesignWhenReceivingADesignUpdateRequestedMessage(List.of(designInsertRequestedMessage, designUpdateRequestedMessage).get(0), List.of(designInsertRequestedMessage, designUpdateRequestedMessage).get(1));
        }

        @Test
        @PactTestFor(providerName = "designs-event-producer", port = "1113", pactMethod = "designDeleteRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a DesignDeleteRequested event")
        public void shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage designDeleteRequestedMessage = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            testCases.shouldUpdateTheDesignWhenReceivingADesignDeleteRequestedMessage(List.of(designInsertRequestedMessage, designDeleteRequestedMessage).get(0), List.of(designInsertRequestedMessage, designDeleteRequestedMessage).get(1));
        }

        @Test
        @PactTestFor(providerName = "designs-tile-renderer", port = "1114", pactMethod = "tileRenderCompleted", providerType = ProviderType.ASYNCH)
        @DisplayName("Should update the design after receiving a TileRenderCompleted event")
        public void shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord3 = Json.decodeValue(messagePact.getMessages().get(2).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord4 = Json.decodeValue(messagePact.getMessages().get(3).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord5 = Json.decodeValue(messagePact.getMessages().get(4).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord6 = Json.decodeValue(messagePact.getMessages().get(5).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage1 = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage2 = OutputMessage.from(kafkaRecord3.getKey(), Json.decodeValue(kafkaRecord3.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage3 = OutputMessage.from(kafkaRecord4.getKey(), Json.decodeValue(kafkaRecord4.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage4 = OutputMessage.from(kafkaRecord5.getKey(), Json.decodeValue(kafkaRecord5.getValue(), Payload.class));

            final OutputMessage tileRenderCompletedMessage5 = OutputMessage.from(kafkaRecord6.getKey(), Json.decodeValue(kafkaRecord6.getValue(), Payload.class));

            final List<OutputMessage> tileRenderCompletedMessages = List.of(tileRenderCompletedMessage1, tileRenderCompletedMessage2, tileRenderCompletedMessage3, tileRenderCompletedMessage4, tileRenderCompletedMessage5);

            testCases.shouldUpdateTheDesignWhenReceivingATileRenderCompletedMessage(designInsertRequestedMessage, tileRenderCompletedMessages);
        }
    }

    @Nested
    @Tag("pact")
    @DisplayName("Verify contract between designs-event-consumer and designs-notification-dispatcher")
    @Provider("designs-event-consumer")
    @Consumer("designs-notification-dispatcher")
    @PactBroker
    public class VerifyDesignsNotificationDispatcher {
        @BeforeEach
        public void before(PactVerificationContext context) {
            context.setTarget(new AmpqTestTarget());
        }

        @TestTemplate
        @ExtendWith(PactVerificationInvocationContextProvider.class)
        @DisplayName("Verify interaction")
        public void pactVerificationTestTemplate(PactVerificationContext context) {
        }

        @State("kafka topic exists")
        public void kafkaTopicExists() {
        }

        @PactVerifyProvider("design changed event 1")
        public String produceDesignAggregateUpdateCompleted1() {
            return testCases.produceDesignAggregateUpdateCompleted1();
        }

        @PactVerifyProvider("design changed event 2")
        public String produceDesignAggregateUpdateCompleted2() {
            return testCases.produceDesignAggregateUpdateCompleted2();
        }
    }
}
