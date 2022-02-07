package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

@Tag("slow")
@Tag("pact")
@DisplayName("Test designs-query pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static TestCases testCases = new TestCases("PactTests");

    @BeforeAll
    public static void before() {
        testCases.before();

        System.setProperty("pact.showStacktrace", "true");
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", testCases.getVersion());
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @Pact(consumer = "designs-query", provider = "designs-aggregate")
    public MessagePact designDocumentUpdateRequested(MessagePactBuilder builder) {
        final UUID uuid1 = new UUID(0L, 1L);
        final UUID uuid2 = new UUID(0L, 2L);

        PactDslJsonArray tiles1 = new PactDslJsonArray()
                .object()
                .numberValue("level", 0)
                .numberValue("requested", 1)
                .array("completed")
                .numberValue(1)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("requested", 4)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("requested", 16)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .numberValue(11)
                .numberValue(12)
                .numberValue(13)
                .numberValue(14)
                .numberValue(21)
                .numberValue(22)
                .numberValue(23)
                .numberValue(24)
                .numberValue(31)
                .numberValue(32)
                .numberValue(33)
                .numberValue(34)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .asArray();

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("uuid", uuid1)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("json", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "CREATED")
                .numberValue("levels", TestConstants.LEVELS)
                .date("modified", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .object("tiles", tiles1);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid1.toString())
                .object("value", payload1);

        PactDslJsonArray tiles2 = new PactDslJsonArray()
                .object()
                .numberValue("level", 0)
                .numberValue("requested", 1)
                .array("completed")
                .numberValue(1)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("requested", 4)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("requested", 16)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .numberValue(11)
                .numberValue(12)
                .numberValue(13)
                .numberValue(14)
                .numberValue(21)
                .numberValue(22)
                .numberValue(23)
                .numberValue(24)
                .numberValue(31)
                .numberValue(32)
                .numberValue(33)
                .numberValue(34)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .asArray();

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("uuid", uuid1)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("json", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "UPDATED")
                .numberValue("levels", TestConstants.LEVELS)
                .date("modified", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .object("tiles", tiles2);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid1.toString())
                .object("value", payload2);

        PactDslJsonArray tiles3 = new PactDslJsonArray()
                .object()
                .numberValue("level", 0)
                .numberValue("requested", 1)
                .array("completed")
                .numberValue(1)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("requested", 4)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("requested", 16)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .numberValue(11)
                .numberValue(12)
                .numberValue(13)
                .numberValue(14)
                .numberValue(21)
                .numberValue(22)
                .numberValue(23)
                .numberValue(24)
                .numberValue(31)
                .numberValue(32)
                .numberValue(33)
                .numberValue(34)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .asArray();

        PactDslJsonBody event3 = new PactDslJsonBody()
                .uuid("uuid", uuid1)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("json", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "UPDATED")
                .numberValue("levels", TestConstants.LEVELS)
                .date("modified", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .object("tiles", tiles3);

        PactDslJsonBody payload3 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event3)
                .stringValue("type", TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message3 = new PactDslJsonBody()
                .stringValue("key", uuid1.toString())
                .object("value", payload3);

        PactDslJsonArray tiles4 = new PactDslJsonArray()
                .object()
                .numberValue("level", 0)
                .numberValue("requested", 1)
                .array("completed")
                .numberValue(1)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("requested", 4)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("requested", 16)
                .array("completed")
                .numberValue(1)
                .numberValue(2)
                .numberValue(3)
                .numberValue(4)
                .numberValue(11)
                .numberValue(12)
                .numberValue(13)
                .numberValue(14)
                .numberValue(21)
                .numberValue(22)
                .numberValue(23)
                .numberValue(24)
                .numberValue(31)
                .numberValue(32)
                .numberValue(33)
                .numberValue(34)
                .closeArray()
                .array("failed")
                .closeArray()
                .closeObject()
                .asArray();

        PactDslJsonBody event4 = new PactDslJsonBody()
                .uuid("uuid", uuid2)
                .stringMatcher("evid", TestConstants.UUID1_REGEXP)
                .stringValue("json", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "UPDATED")
                .numberValue("levels", TestConstants.LEVELS)
                .date("modified", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .object("tiles", tiles4);

        PactDslJsonBody payload4 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event4)
                .stringValue("type", TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message4 = new PactDslJsonBody()
                .stringValue("key", uuid2.toString())
                .object("value", payload4);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design document update requested for design 00000000-0000-0000-0000-000000000001 and 0% tiles completed")
                .withContent(message1)
                .expectsToReceive("design document update requested for design 00000000-0000-0000-0000-000000000001 and 50% tiles completed")
                .withContent(message2)
                .expectsToReceive("design document update requested for design 00000000-0000-0000-0000-000000000001 and 100% tiles completed")
                .withContent(message3)
                .expectsToReceive("design document update requested for design 00000000-0000-0000-0000-000000000002 and 100% tiles completed")
                .withContent(message4)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-query", port = "1111", pactMethod = "designDocumentUpdateRequested", providerType = ProviderType.ASYNCH)
    @DisplayName("Should update the design after receiving a DesignDocumentUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(MessagePact messagePact) {
        final OutputMessage designDocumentUpdateRequested1 = TestUtils.toOutputMessage(messagePact.getMessages().get(0));
        final OutputMessage designDocumentUpdateRequested2 = TestUtils.toOutputMessage(messagePact.getMessages().get(1));
        final OutputMessage designDocumentUpdateRequested3 = TestUtils.toOutputMessage(messagePact.getMessages().get(2));
        final OutputMessage designDocumentUpdateRequested4 = TestUtils.toOutputMessage(messagePact.getMessages().get(3));

        final List<OutputMessage> outputMessages = List.of(designDocumentUpdateRequested1, designDocumentUpdateRequested2, designDocumentUpdateRequested3, designDocumentUpdateRequested4);

        testCases.shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(outputMessages);
    }
}
