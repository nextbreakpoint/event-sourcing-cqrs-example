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
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

@Tag("slow")
@Tag("pact-verify")
@Provider("designs-tile-renderer")
@Consumer("designs-event-consumer")
@DisplayName("Verify contract between designs-tile-renderer and designs-event-consumer")
@PactBroker
public class VerifyEventConsumerPact {
    public VerifyEventConsumerPact() {
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

    @PactVerifyProvider("tile render completed with status FAILED for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
    public String produceTileRenderCompleted1() {
        return produceTileRenderCompleted(new UUID(0L, 4L), 0, 0, 0, TestConstants.CHECKSUM_1, "FAILED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 1/00010000.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
    public String produceTileRenderCompleted2() {
        return produceTileRenderCompleted(new UUID(0L, 4L), 1, 0, 0, TestConstants.CHECKSUM_1, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 1/00010001.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
    public String produceTileRenderCompleted3() {
        return produceTileRenderCompleted(new UUID(0L, 4L), 1, 1, 0, TestConstants.CHECKSUM_1, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 2/00020001.png of design 00000000-0000-0000-0000-000000000004 with checksum 1")
    public String produceTileRenderCompleted4() {
        return produceTileRenderCompleted(new UUID(0L, 4L), 2, 2, 1, TestConstants.CHECKSUM_1, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status FAILED for tile 2/00030001.png of design 00000000-0000-0000-0000-000000000004 with checksum 2")
    public String produceTileRenderCompleted5() {
        return produceTileRenderCompleted(new UUID(0L, 4L), 2, 3, 1, TestConstants.CHECKSUM_2, "FAILED");
    }

    private String produceTileRenderCompleted(UUID uuid, int level, int row, int col, String checksum, String status) {
        final TileRenderCompleted tileRenderCompleted = new TileRenderCompleted(Uuids.timeBased(), uuid, 0, checksum, level, row, col, status);

        final OutputMessage tileRenderCompletedMessage = new TileRenderCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(tileRenderCompleted);

        return Json.encode(new KafkaRecord(tileRenderCompletedMessage.getKey(), PayloadUtils.payloadToMap(tileRenderCompletedMessage.getValue())));
    }
}
