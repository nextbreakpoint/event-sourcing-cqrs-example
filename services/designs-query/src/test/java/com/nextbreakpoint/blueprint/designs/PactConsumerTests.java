package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
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
                .numberValue("total", 1)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("total", 4)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("total", 16)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 3)
                .numberValue("total", 64)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 4)
                .numberValue("total", 256)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 5)
                .numberValue("total", 1024)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 6)
                .numberValue("total", 4096)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 7)
                .numberValue("total", 16384)
                .numberValue("completed", 0)
                .closeObject()
                .asArray();

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid1)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "CREATED")
                .booleanValue("published", false)
                .numberValue("levels", TestConstants.LEVELS)
                .date("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .date("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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
                .numberValue("total", 1)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("total", 4)
                .numberValue("completed", 2)
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("total", 16)
                .numberValue("completed", 8)
                .closeObject()
                .object()
                .numberValue("level", 3)
                .numberValue("total", 64)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 4)
                .numberValue("total", 256)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 5)
                .numberValue("total", 1024)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 6)
                .numberValue("total", 4096)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 7)
                .numberValue("total", 16384)
                .numberValue("completed", 0)
                .closeObject()
                .asArray();

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid1)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "UPDATED")
                .booleanValue("published", false)
                .numberValue("levels", TestConstants.LEVELS)
                .date("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .date("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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
                .numberValue("total", 1)
                .numberValue("completed", 1)
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("total", 4)
                .numberValue("completed", 4)
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("total", 16)
                .numberValue("completed", 16)
                .closeObject()
                .object()
                .numberValue("level", 3)
                .numberValue("total", 64)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 4)
                .numberValue("total", 256)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 5)
                .numberValue("total", 1024)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 6)
                .numberValue("total", 4096)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 7)
                .numberValue("total", 16384)
                .numberValue("completed", 0)
                .closeObject()
                .asArray();

        PactDslJsonBody event3 = new PactDslJsonBody()
                .uuid("designId", uuid1)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_1)
                .stringValue("checksum", TestConstants.CHECKSUM_1)
                .stringValue("status", "UPDATED")
                .booleanValue("published", false)
                .numberValue("levels", TestConstants.LEVELS)
                .date("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .date("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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
                .numberValue("total", 1)
                .numberValue("completed", 1)
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("total", 4)
                .numberValue("completed", 4)
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("total", 16)
                .numberValue("completed", 16)
                .closeObject()
                .object()
                .numberValue("level", 3)
                .numberValue("total", 64)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 4)
                .numberValue("total", 256)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 5)
                .numberValue("total", 1024)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 6)
                .numberValue("total", 4096)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 7)
                .numberValue("total", 16384)
                .numberValue("completed", 0)
                .closeObject()
                .asArray();

        PactDslJsonBody event4 = new PactDslJsonBody()
                .uuid("designId", uuid2)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .stringValue("status", "UPDATED")
                .booleanValue("published", false)
                .numberValue("levels", TestConstants.LEVELS)
                .date("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .date("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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

    @Pact(consumer = "designs-query", provider = "designs-aggregate")
    public MessagePact designDocumentDeleteRequested(MessagePactBuilder builder) {
        final UUID uuid = new UUID(0L, 3L);

        PactDslJsonArray tiles = new PactDslJsonArray()
                .object()
                .numberValue("level", 0)
                .numberValue("total", 1)
                .numberValue("completed", 1)
                .closeObject()
                .object()
                .numberValue("level", 1)
                .numberValue("total", 4)
                .numberValue("completed", 4)
                .closeObject()
                .object()
                .numberValue("level", 2)
                .numberValue("total", 16)
                .numberValue("completed", 16)
                .closeObject()
                .object()
                .numberValue("level", 3)
                .numberValue("total", 64)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 4)
                .numberValue("total", 256)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 5)
                .numberValue("total", 1024)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 6)
                .numberValue("total", 4096)
                .numberValue("completed", 0)
                .closeObject()
                .object()
                .numberValue("level", 7)
                .numberValue("total", 16384)
                .numberValue("completed", 0)
                .closeObject()
                .asArray();

        PactDslJsonBody event1 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .uuid("userId", TestConstants.USER_ID)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP)
                .stringValue("data", TestConstants.JSON_2)
                .stringValue("checksum", TestConstants.CHECKSUM_2)
                .stringValue("status", "CREATED")
                .booleanValue("published", false)
                .numberValue("levels", TestConstants.LEVELS)
                .date("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .date("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .object("tiles", tiles);

        PactDslJsonBody payload1 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event1)
                .stringValue("type", TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message1 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload1);

        PactDslJsonBody event2 = new PactDslJsonBody()
                .uuid("designId", uuid)
                .stringMatcher("commandId", TestConstants.UUID6_REGEXP)
                .stringMatcher("revision", TestConstants.REVISION_REGEXP);

        PactDslJsonBody payload2 = new PactDslJsonBody()
                .stringMatcher("uuid", TestConstants.UUID6_REGEXP)
                .object("data", event2)
                .stringValue("type", TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED)
                .stringValue("source", TestConstants.MESSAGE_SOURCE);

        PactDslJsonBody message2 = new PactDslJsonBody()
                .stringValue("key", uuid.toString())
                .object("value", payload2);

        return builder
                .given("kafka topic exists")
                .expectsToReceive("design document update requested for design 00000000-0000-0000-0000-000000000003 and 100% tiles completed")
                .withContent(message1)
                .expectsToReceive("design document delete requested for design 00000000-0000-0000-0000-000000000003")
                .withContent(message2)
                .toPact();
    }

    @Test
    @PactTestFor(providerName = "designs-query", port = "1111", pactMethod = "designDocumentUpdateRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    @DisplayName("Should update the design after receiving a DesignDocumentUpdateRequested event")
    public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(MessagePact pact) {
        assertThat(pact.getMessages()).hasSize(4);

        final OutputMessage designDocumentUpdateRequestedMessage1 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(0)));
        final OutputMessage designDocumentUpdateRequestedMessage2 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(1)));
        final OutputMessage designDocumentUpdateRequestedMessage3 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(2)));
        final OutputMessage designDocumentUpdateRequestedMessage4 = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(3)));

        final List<OutputMessage> outputMessages = List.of(designDocumentUpdateRequestedMessage1, designDocumentUpdateRequestedMessage2, designDocumentUpdateRequestedMessage3, designDocumentUpdateRequestedMessage4);

        testCases.shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(outputMessages);
    }

    @Test
    @PactTestFor(providerName = "designs-query", port = "1112", pactMethod = "designDocumentDeleteRequested", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    @DisplayName("Should delete the design after receiving a DesignDocumentDeleteRequested event")
    public void shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage(MessagePact pact) {
        assertThat(pact.getMessages()).hasSize(2);

        final OutputMessage designDocumentUpdateRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(0)));
        final OutputMessage designDocumentDeleteRequestedMessage = TestUtils.toOutputMessage(Objects.requireNonNull(pact.getMessages().get(1)));

        testCases.shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage(designDocumentUpdateRequestedMessage, designDocumentDeleteRequestedMessage);
    }
}