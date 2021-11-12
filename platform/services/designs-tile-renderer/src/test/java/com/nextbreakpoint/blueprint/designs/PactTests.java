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
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PactTests {
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
    @DisplayName("Test designs-tile-renderer pact")
    @ExtendWith(PactConsumerTestExt.class)
    public class TestTileRendererConsumer {
        @Pact(consumer = "designs-tile-renderer")
        public MessagePact tileRenderRequested(MessagePactBuilder builder) {
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
                    .stringValue("data", TestConstants.JSON_1)
                    .stringValue("checksum", TestConstants.CHECKSUM_1)
                    .numberValue("level", 0)
                    .numberValue("row", 0)
                    .numberValue("col", 0);

            PactDslJsonBody value2 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload2.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message2 = new PactDslJsonBody()
                    .stringValue("key", TestConstants.CHECKSUM_1 + "/0/00000000.png")
                    .stringValue("value", value2.toString());

            PactDslJsonBody payload3 = new PactDslJsonBody()
                    .uuid("uuid", uuid)
                    .uuid("evid", Uuids.timeBased())
                    .numberValue("esid", 0)
                    .stringValue("data", TestConstants.JSON_2)
                    .stringValue("checksum", TestConstants.CHECKSUM_2)
                    .numberValue("level", 1)
                    .numberValue("row", 1)
                    .numberValue("col", 2);

            PactDslJsonBody value3 = new PactDslJsonBody()
                    .uuid("uuid")
                    .stringValue("data", payload3.toString())
                    .stringValue("type", TestConstants.TILE_RENDER_REQUESTED)
                    .stringValue("source", TestConstants.MESSAGE_SOURCE);

            PactDslJsonBody message3 = new PactDslJsonBody()
                    .stringValue("key", TestConstants.CHECKSUM_2 + "/1/00010002.png")
                    .stringValue("value", value3.toString());

            return builder.given("kafka topic exists")
                    .expectsToReceive("design insert requested")
                    .withContent(message1)
                    .expectsToReceive("tile render requested")
                    .withContent(message2)
                    .expectsToReceive("tile render requested")
                    .withContent(message3)
                    .toPact();
        }

        @Test
        @PactTestFor(providerName = "designs-tile-renderer", port = "1111", pactMethod = "tileRenderRequested", providerType = ProviderType.ASYNCH)
        @DisplayName("Should start rendering an image after receiving a TileRenderRequested event")
        public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(MessagePact messagePact) {
            final KafkaRecord kafkaRecord1 = Json.decodeValue(messagePact.getMessages().get(0).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord2 = Json.decodeValue(messagePact.getMessages().get(1).contentsAsString(), KafkaRecord.class);

            final KafkaRecord kafkaRecord3 = Json.decodeValue(messagePact.getMessages().get(2).contentsAsString(), KafkaRecord.class);

            final OutputMessage designInsertRequestedMessage = OutputMessage.from(kafkaRecord1.getKey(), Json.decodeValue(kafkaRecord1.getValue(), Payload.class));

            final OutputMessage tileRenderRequestedMessage1 = OutputMessage.from(kafkaRecord2.getKey(), Json.decodeValue(kafkaRecord2.getValue(), Payload.class));

            final OutputMessage tileRenderRequestedMessage2 = OutputMessage.from(kafkaRecord3.getKey(), Json.decodeValue(kafkaRecord3.getValue(), Payload.class));

            testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(designInsertRequestedMessage, List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2));
        }
    }

    @Nested
    @Tag("pact")
    @DisplayName("Verify contract between designs-tile-renderer and designs-event-consumer")
    @Provider("designs-tile-renderer")
    @Consumer("designs-event-consumer")
    @PactBroker
    public class VerifyDesignsEventConsumer {
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

        @PactVerifyProvider("tile render completed event 1")
        public String produceTileRenderCompleted1() {
            return testCases.produceTileRenderCompleted1();
        }

        @PactVerifyProvider("tile render completed event 2")
        public String produceTileRenderCompleted2() {
            return testCases.produceTileRenderCompleted2();
        }
    }
}
