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
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileStatus;
import com.nextbreakpoint.blueprint.common.test.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import com.nextbreakpoint.blueprint.designs.common.Render;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.CHECKSUM_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.CHECKSUM_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_5;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_5;

@Tag("docker")
@Tag("pact-verify")
@Provider("designs-render")
@Consumer("designs-aggregate")
@DisplayName("Verify contract between designs-render and designs-aggregate")
@PactBroker
public class VerifyAggregatePact {
    public VerifyAggregatePact() {
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

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000001 and command 00000000-0000-0001-0000-000000000003")
    public String produceTileRenderCompleted0() {
        return produceTileRenderCompleted(DESIGN_ID_1, COMMAND_ID_3, CHECKSUM_1, REVISION_0, 0, 0, 0, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status FAILED for tile 0/00000000.png of design 00000000-0000-0000-0000-000000000002 and command 00000000-0000-0001-0000-000000000004")
    public String produceTileRenderCompleted1() {
        return produceTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_4, CHECKSUM_2, REVISION_1, 0, 0, 0, "FAILED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 1/00010000.png of design 00000000-0000-0000-0000-000000000002 and command 00000000-0000-0001-0000-000000000004")
    public String produceTileRenderCompleted2() {
        return produceTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_4, CHECKSUM_2, REVISION_2, 1, 0, 0, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 1/00010001.png of design 00000000-0000-0000-0000-000000000002 and command 00000000-0000-0001-0000-000000000004")
    public String produceTileRenderCompleted3() {
        return produceTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_4, CHECKSUM_2, REVISION_3, 1, 1, 0, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 2/00020001.png of design 00000000-0000-0000-0000-000000000002 and command 00000000-0000-0001-0000-000000000004")
    public String produceTileRenderCompleted4() {
        return produceTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_4, CHECKSUM_2, REVISION_4, 2, 2, 1, "COMPLETED");
    }

    @PactVerifyProvider("tile render completed with status COMPLETED for tile 2/00020002.png of design 00000000-0000-0000-0000-000000000002 and command 00000000-0000-0001-0000-000000000005")
    public String produceTileRenderCompleted5() {
        return produceTileRenderCompleted(DESIGN_ID_2, COMMAND_ID_5, CHECKSUM_2, REVISION_5, 2, 2, 2, "COMPLETED");
    }

    private String produceTileRenderCompleted(UUID designId, UUID commandId, String checksum, String revision, int level, int row, int col, String status) {
        final TileRenderCompleted tileRenderCompleted = new TileRenderCompleted(designId, commandId, revision, checksum, TileStatus.valueOf(status), level, row, col);

        final OutputMessage<TileRenderCompleted> tileRenderCompletedMessage = MessageFactory.<TileRenderCompleted>of(MESSAGE_SOURCE).createOutputMessage(tileRenderCompleted.getDesignId().toString(), tileRenderCompleted);

        return Json.encodeValue(new KafkaRecord(Render.createRenderKey(tileRenderCompleted), PayloadUtils.payloadToMap(tileRenderCompletedMessage.getValue())));
    }
}
