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

    @PactVerifyProvider("design insert requested for design 00000000-0000-0000-0000-000000000001")
    public String produceDesignInsertRequested1() {
        return produceDesignInsertRequested(new UUID(0L, 1L), TestConstants.JSON_1);
    }

    @PactVerifyProvider("design insert requested for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignInsertRequested2() {
        return produceDesignInsertRequested(new UUID(0L, 2L), TestConstants.JSON_1);
    }

    @PactVerifyProvider("design insert requested for design 00000000-0000-0000-0000-000000000003")
    public String produceDesignInsertRequested3() {
        return produceDesignInsertRequested(new UUID(0L, 3L), TestConstants.JSON_1);
    }

    @PactVerifyProvider("design update requested for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignUpdateRequested1() {
        return produceDesignUpdateRequested(new UUID(0L, 2L), TestConstants.JSON_2);
    }

    @PactVerifyProvider("design delete requested for design 00000000-0000-0000-0000-000000000003")
    public String produceDesignDeleteRequested1() {
        return produceDesignDeleteRequested(new UUID(0L, 3L));
    }

    private String produceDesignInsertRequested(UUID designId, String data) {
        final DesignInsertRequested designInsertRequested = new DesignInsertRequested(designId, UUID.randomUUID(), TestConstants.USER_ID, data);

        final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

        return Json.encodeValue(new KafkaRecord(designInsertRequestedMessage.getKey(), PayloadUtils.payloadToMap(designInsertRequestedMessage.getValue())));
    }

    private String produceDesignUpdateRequested(UUID designId, String data) {
        final DesignUpdateRequested designUpdateRequested = new DesignUpdateRequested(designId, UUID.randomUUID(), TestConstants.USER_ID, data, false);

        final OutputMessage designUpdateRequestedMessage = new DesignUpdateRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designUpdateRequested);

        return Json.encodeValue(new KafkaRecord(designUpdateRequestedMessage.getKey(), PayloadUtils.payloadToMap(designUpdateRequestedMessage.getValue())));
    }

    private String produceDesignDeleteRequested(UUID designId) {
        final DesignDeleteRequested designDeleteRequested = new DesignDeleteRequested(designId, UUID.randomUUID(), TestConstants.USER_ID);

        final OutputMessage designDeleteRequestedMessage = new DesignDeleteRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDeleteRequested);

        return Json.encodeValue(new KafkaRecord(designDeleteRequestedMessage.getKey(), PayloadUtils.payloadToMap(designDeleteRequestedMessage.getValue())));
    }
}