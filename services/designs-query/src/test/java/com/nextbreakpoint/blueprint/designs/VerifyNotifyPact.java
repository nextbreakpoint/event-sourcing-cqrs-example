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
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
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
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-query and designs-watch")
@Provider("designs-query")
@Consumer("designs-watch")
@PactBroker
public class VerifyNotifyPact {
    public VerifyNotifyPact() {
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

    @PactVerifyProvider("design document update completed for design 00000000-0000-0000-0000-000000000001")
    public String produceDesignDocumentUpdateCompleted1() {
        return produceDesignDocumentUpdateCompleted(DESIGN_ID_1, COMMAND_ID_1, REVISION_0);
    }

    @PactVerifyProvider("design document update completed for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignDocumentUpdateCompleted2() {
        return produceDesignDocumentUpdateCompleted(DESIGN_ID_2, COMMAND_ID_2, REVISION_1);
    }

    @PactVerifyProvider("design document delete completed for design 00000000-0000-0000-0000-000000000001")
    public String produceDesignDocumentDeleteCompleted1() {
        return produceDesignDocumentDeleteCompleted(DESIGN_ID_1, COMMAND_ID_3, REVISION_0);
    }

    @PactVerifyProvider("design document delete completed for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignDocumentDeleteCompleted2() {
        return produceDesignDocumentDeleteCompleted(DESIGN_ID_2, COMMAND_ID_4, REVISION_1);
    }

    private String produceDesignDocumentUpdateCompleted(UUID designId, UUID commandId, String revision) {
        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted = new DesignDocumentUpdateCompleted(designId, commandId, revision);

        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage = MessageFactory.<DesignDocumentUpdateCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentUpdateCompleted.getDesignId().toString(), designDocumentUpdateCompleted);

        return Json.encodeValue(new KafkaRecord(designDocumentUpdateCompletedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentUpdateCompletedMessage.getValue())));
    }

    private String produceDesignDocumentDeleteCompleted(UUID designId, UUID commandId, String revision) {
        final DesignDocumentDeleteCompleted designDocumentDeleteCompleted = new DesignDocumentDeleteCompleted(designId, commandId, revision);

        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage = MessageFactory.<DesignDocumentDeleteCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentDeleteCompleted.getDesignId().toString(), designDocumentDeleteCompleted);

        return Json.encodeValue(new KafkaRecord(designDocumentDeleteCompletedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentDeleteCompletedMessage.getValue())));
    }
}
