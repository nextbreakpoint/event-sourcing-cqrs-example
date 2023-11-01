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
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-command and designs-aggregate")
@Provider("designs-command")
@Consumer("designs-aggregate")
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

    @PactVerifyProvider("design insert requested for design 00000000-0000-0000-0000-000000000001 with command 00000000-0000-0001-0000-000000000001")
    public String produceDesignInsertRequested1() {
        return produceDesignInsertRequested(USER_ID_1, DESIGN_ID_1, COMMAND_ID_1, DATA_1);
    }

    @PactVerifyProvider("design insert requested for design 00000000-0000-0000-0000-000000000002 with command 00000000-0000-0001-0000-000000000001")
    public String produceDesignInsertRequested2() {
        return produceDesignInsertRequested(USER_ID_1, DESIGN_ID_2, COMMAND_ID_1, DATA_1);
    }

    @PactVerifyProvider("design insert requested for design 00000000-0000-0000-0000-000000000003 with command 00000000-0000-0001-0000-000000000001")
    public String produceDesignInsertRequested3() {
        return produceDesignInsertRequested(USER_ID_1, DESIGN_ID_3, COMMAND_ID_1, DATA_3);
    }

    @PactVerifyProvider("design update requested for design 00000000-0000-0000-0000-000000000002 with command 00000000-0000-0001-0000-000000000002")
    public String produceDesignUpdateRequested1() {
        return produceDesignUpdateRequested(USER_ID_1, DESIGN_ID_2, COMMAND_ID_2, DATA_2, false);
    }

    @PactVerifyProvider("design update requested for design 00000000-0000-0000-0000-000000000002 with command 00000000-0000-0001-0000-000000000003 and published true")
    public String produceDesignUpdateRequested2() {
        return produceDesignUpdateRequested(USER_ID_1, DESIGN_ID_2, COMMAND_ID_3, DATA_2, true);
    }

    @PactVerifyProvider("design delete requested for design 00000000-0000-0000-0000-000000000003 with command 00000000-0000-0001-0000-000000000002")
    public String produceDesignDeleteRequested1() {
        return produceDesignDeleteRequested(USER_ID_2, DESIGN_ID_3, COMMAND_ID_2);
    }

    private String produceDesignInsertRequested(UUID userId, UUID designId, UUID commandId, String data) {
        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, commandId, userId, data);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(MESSAGE_SOURCE).transform(designInsertRequested);

        return Json.encodeValue(new KafkaRecord(designInsertRequestedMessage.getKey(), PayloadUtils.payloadToMap(designInsertRequestedMessage.getValue())));
    }

    private String produceDesignUpdateRequested(UUID userId, UUID designId, UUID commandId, String data, boolean published) {
        final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(designId, commandId, userId, data, published);

        final OutputMessage designUpdateRequestedMessage = new DesignUpdateRequestedOutputMapper(MESSAGE_SOURCE).transform(designUpdateRequested);

        return Json.encodeValue(new KafkaRecord(designUpdateRequestedMessage.getKey(), PayloadUtils.payloadToMap(designUpdateRequestedMessage.getValue())));
    }

    private String produceDesignDeleteRequested(UUID userId, UUID designId, UUID commandId) {
        final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(designId, commandId, userId);

        final OutputMessage designDeleteRequestedMessage = new DesignDeleteRequestedOutputMapper(MESSAGE_SOURCE).transform(designDeleteRequested);

        return Json.encodeValue(new KafkaRecord(designDeleteRequestedMessage.getKey(), PayloadUtils.payloadToMap(designDeleteRequestedMessage.getValue())));
    }
}
