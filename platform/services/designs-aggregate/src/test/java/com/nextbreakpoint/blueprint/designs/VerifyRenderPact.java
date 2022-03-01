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
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-aggregate and designs-render")
@Provider("designs-aggregate")
@Consumer("designs-render")
@PactBroker
public class VerifyRenderPact {
    public VerifyRenderPact() {
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

    @PactVerifyProvider("tile render requested for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
    public String produceTileRenderRequested1() {
        return produceTileRenderRequested(new UUID(0L, 4L), 0, 0, 0, TestConstants.JSON_1, TestConstants.CHECKSUM_1);
    }

    @PactVerifyProvider("tile render requested for tile 1/00010002.png of design 00000000-0000-0000-0000-000000000004 with checksum 2")
    public String produceTileRenderRequested2() {
        return produceTileRenderRequested(new UUID(0L, 4L), 1, 1, 2, TestConstants.JSON_2, TestConstants.CHECKSUM_2);
    }

    private String produceTileRenderRequested(UUID uuid, int level, int row, int col, String data, String checksum) {
        final TileRenderRequested tileRenderRequested = new TileRenderRequested(TimeUUID.next(), uuid, TestConstants.REVISION_0, data, checksum, level,  row, col);

        final OutputMessage tileRenderRequestedMessage = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, event -> TestUtils.createBucketKey(tileRenderRequested)).transform(Tracing.of(UUID.randomUUID()), tileRenderRequested);

        return Json.encodeValue(new KafkaRecord(tileRenderRequestedMessage.getKey(), PayloadUtils.payloadToMap(tileRenderRequestedMessage.getValue()), tileRenderRequestedMessage.getTrace().toHeaders()));
    }
}
