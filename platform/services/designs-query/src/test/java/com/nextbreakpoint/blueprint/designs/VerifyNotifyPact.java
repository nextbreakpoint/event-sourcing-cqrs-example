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
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-query and designs-notify")
@Provider("designs-query")
@Consumer("designs-notify")
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
        return produceDesignDocumentUpdateCompleted(new UUID(0L, 1L));
    }

    @PactVerifyProvider("design document update completed for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignDocumentUpdateCompleted2() {
        return produceDesignDocumentUpdateCompleted(new UUID(0L, 2L));
    }

    @PactVerifyProvider("design document delete completed for design 00000000-0000-0000-0000-000000000001")
    public String produceDesignDocumentDeleteCompleted1() {
        return produceDesignDocumentDeleteCompleted(new UUID(0L, 1L));
    }

    @PactVerifyProvider("design document delete completed for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignDocumentDeleteCompleted2() {
        return produceDesignDocumentDeleteCompleted(new UUID(0L, 2L));
    }

    private String produceDesignDocumentUpdateCompleted(UUID uuid) {
        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted = new DesignDocumentUpdateCompleted(uuid, TestConstants.REVISION_0);

        final OutputMessage designDocumentUpdateCompletedMessage = new DesignDocumentUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(Tracing.of(UUID.randomUUID()), designDocumentUpdateCompleted);

        return Json.encodeValue(new KafkaRecord(designDocumentUpdateCompletedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentUpdateCompletedMessage.getValue()), designDocumentUpdateCompletedMessage.getTrace().toHeaders()));
    }

    private String produceDesignDocumentDeleteCompleted(UUID uuid) {
        final DesignDocumentDeleteCompleted designDocumentDeleteCompleted = new DesignDocumentDeleteCompleted(uuid, TestConstants.REVISION_0);

        final OutputMessage designDocumentDeleteCompletedMessage = new DesignDocumentDeleteCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(Tracing.of(UUID.randomUUID()), designDocumentDeleteCompleted);

        return Json.encodeValue(new KafkaRecord(designDocumentDeleteCompletedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentDeleteCompletedMessage.getValue()), designDocumentDeleteCompletedMessage.getTrace().toHeaders()));
    }
}
