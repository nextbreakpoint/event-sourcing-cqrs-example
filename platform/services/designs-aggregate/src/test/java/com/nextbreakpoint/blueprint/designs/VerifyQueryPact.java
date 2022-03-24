package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.designs.model.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-aggregate and designs-query")
@Provider("designs-aggregate")
@Consumer("designs-query")
@PactBroker
public class VerifyQueryPact {
    public VerifyQueryPact() {
        TestScenario scenario = new TestScenario();

        System.setProperty("pact.showStacktrace", "true");
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", scenario.getVersion());
    }

    @BeforeEach
    public void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    @DisplayName("Verify interaction")
    public void pactVerificationTestTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
        System.out.println("TestTemplate called: " + pact.getProvider().getName() + ", " + interaction);
        context.verifyInteraction();
    }

    @State("kafka topic exists")
    public void kafkaTopicExists() {
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000001 and 0% tiles completed")
    public String produceDesignDocumentUpdateRequested1() {
        return produceDesignDocumentUpdateRequested(new UUID(0L, 1L), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED", 0.0f);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000001 and 50% tiles completed")
    public String produceDesignDocumentUpdateRequested2() {
        return produceDesignDocumentUpdateRequested(new UUID(0L, 1L), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "UPDATED", 0.5f);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000001 and 100% tiles completed")
    public String produceDesignDocumentUpdateRequested3() {
        return produceDesignDocumentUpdateRequested(new UUID(0L, 1L), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "UPDATED", 1.0f);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000002 and 100% tiles completed")
    public String produceDesignDocumentUpdateRequested4() {
        return produceDesignDocumentUpdateRequested(new UUID(0L, 2L), TestConstants.JSON_2, TestConstants.CHECKSUM_2, "UPDATED", 1.0f);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000003 and 100% tiles completed")
    public String produceDesignDocumentUpdateRequested5() {
        return produceDesignDocumentUpdateRequested(new UUID(0L, 3L), TestConstants.JSON_2, TestConstants.CHECKSUM_2, "CREATED", 1.0f);
    }

    @PactVerifyProvider("design document delete requested for design 00000000-0000-0000-0000-000000000003")
    public String produceDesignDocumentDeleteRequested() {
        return produceDesignDocumentDeleteRequested(new UUID(0L, 3L));
    }

    private String produceDesignDocumentUpdateRequested(UUID uuid, String data, String checksum, String status, float completePercentage) {
        final List<Tiles> tiles = TestUtils.getTiles(TestConstants.LEVELS, completePercentage).stream().map(Level::toTiles).collect(Collectors.toList());

        final DesignDocumentUpdateRequested designDocumentUpdateRequested = new DesignDocumentUpdateRequested(uuid, TestConstants.USER_ID, UUID.randomUUID(), data, checksum, TestConstants.REVISION_0, status, TestConstants.LEVELS, tiles, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));

        final OutputMessage designDocumentUpdateRequestedMessage = new DesignDocumentUpdateRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentUpdateRequested);

        return Json.encodeValue(new KafkaRecord(designDocumentUpdateRequestedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentUpdateRequestedMessage.getValue())));
    }

    private String produceDesignDocumentDeleteRequested(UUID uuid) {
        final DesignDocumentDeleteRequested designDocumentDeleteRequested = new DesignDocumentDeleteRequested(uuid, TestConstants.REVISION_0);

        final OutputMessage designDocumentDeleteRequestedMessage = new DesignDocumentDeleteRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentDeleteRequested);

        return Json.encodeValue(new KafkaRecord(designDocumentDeleteRequestedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentDeleteRequestedMessage.getValue())));
    }
}
