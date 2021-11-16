package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
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
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

@Tag("pact-verify")
@Tag("verify-circular-pact")
@DisplayName("Verify contract between designs-event-consumer and designs-tile-renderer")
@Provider("designs-event-consumer")
@Consumer("designs-tile-renderer")
@PactBroker
public class VerifyTileRendererPact {
    public VerifyTileRendererPact() {
        TestScenario scenario = new TestScenario();

        System.setProperty("pact.showStacktrace", "true");
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", scenario.getVersion());
    }

    @BeforeEach
    public void before(PactVerificationContext context) {
        context.setTarget(new AmpqTestTarget());
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

    @PactVerifyProvider("tile render requested 1")
    public String produceTileRenderRequested1() {
        return produceTileRenderRequested(new UUID(0L, 4L), 0, 0, 0, TestConstants.JSON_1, TestConstants.CHECKSUM_1);
    }

    @PactVerifyProvider("tile render requested 2")
    public String produceTileRenderRequested2() {
        return produceTileRenderRequested(new UUID(0L, 4L), 1, 1, 2, TestConstants.JSON_2, TestConstants.CHECKSUM_2);
    }

    private String produceTileRenderRequested(UUID uuid, int level, int row, int col, String data, String checksum) {
        final TileRenderRequested tileRenderRequested = new TileRenderRequested(Uuids.timeBased(), uuid, 0, data, checksum, level,  row, col);

        final OutputMessage tileRenderRequestedMessage = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, event -> TestUtils.createBucketKey(tileRenderRequested)).transform(tileRenderRequested);

        return Json.encode(new KafkaRecord(tileRenderRequestedMessage.getKey(), TestUtils.payloadToMap(tileRenderRequestedMessage.getValue())));
    }
}
