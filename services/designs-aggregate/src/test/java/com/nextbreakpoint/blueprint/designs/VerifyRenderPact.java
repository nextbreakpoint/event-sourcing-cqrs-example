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
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;

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

    @PactVerifyProvider("tile render requested for tile 0/00000000 of design 00000000-0000-0000-0000-000000000001 with checksum 1")
    public String produceTileRenderRequested1() {
        return produceTileRenderRequested(DESIGN_ID_1, 0, 0, 0, COMMAND_ID_1, DATA_1, Checksum.of(DATA_1), REVISION_0);
    }

    @PactVerifyProvider("tile render requested for tile 4/00010002 of design 00000000-0000-0000-0000-000000000002 with checksum 2")
    public String produceTileRenderRequested2() {
        return produceTileRenderRequested(DESIGN_ID_2, 4, 1, 2, COMMAND_ID_2, DATA_2, Checksum.of(DATA_2), REVISION_1);
    }

    @PactVerifyProvider("tile render requested for tile 5/00010002 of design 00000000-0000-0000-0000-000000000002 with checksum 2")
    public String produceTileRenderRequested3() {
        return produceTileRenderRequested(DESIGN_ID_2, 5, 1, 2, COMMAND_ID_2, DATA_2, Checksum.of(DATA_2), REVISION_1);
    }

    @PactVerifyProvider("tile render requested for tile 6/00010002 of design 00000000-0000-0000-0000-000000000002 with checksum 2")
    public String produceTileRenderRequested4() {
        return produceTileRenderRequested(DESIGN_ID_2, 6, 1, 2, COMMAND_ID_2, DATA_2, Checksum.of(DATA_2), REVISION_1);
    }

    @PactVerifyProvider("tile render requested for tile 7/00010002 of design 00000000-0000-0000-0000-000000000002 with checksum 2")
    public String produceTileRenderRequested5() {
        return produceTileRenderRequested(DESIGN_ID_2, 7, 1, 2, COMMAND_ID_2, DATA_2, Checksum.of(DATA_2), REVISION_1);
    }

    private String produceTileRenderRequested(UUID designId, int level, int row, int col, UUID commandId, String data, String checksum, String revision) {
        final TileRenderRequested tileRenderRequested = new TileRenderRequested(designId, commandId, revision, checksum, data, level,  row, col);

        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage = MessageFactory.<TileRenderRequested>of(MESSAGE_SOURCE).createOutputMessage(TestUtils.createRenderKey(tileRenderRequested), tileRenderRequested);

        return Json.encodeValue(new KafkaRecord(tileRenderRequestedMessage.getKey(), PayloadUtils.payloadToMap(tileRenderRequestedMessage.getValue())));
    }
}
