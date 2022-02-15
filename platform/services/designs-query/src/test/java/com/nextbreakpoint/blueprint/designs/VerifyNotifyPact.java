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
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

@Tag("slow")
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

    @PactVerifyProvider("design document update completed for design 00000000-0000-0000-0000-000000000001")
    public String produceDesignDocumentUpdateCompleted1() {
        return produceDesignDocumentUpdateCompleted(new UUID(0L, 1L));
    }

    @PactVerifyProvider("design document update completed for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignDocumentUpdateCompleted2() {
        return produceDesignDocumentUpdateCompleted(new UUID(0L, 2L));
    }

    private String produceDesignDocumentUpdateCompleted(UUID uuid) {
        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted = new DesignDocumentUpdateCompleted(Uuids.timeBased(), uuid, 0);

        final OutputMessage designDocumentUpdateCompletedMessage = new DesignDocumentUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(Tracing.of(UUID.randomUUID()), designDocumentUpdateCompleted);

        return Json.encodeValue(new KafkaRecord(designDocumentUpdateCompletedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentUpdateCompletedMessage.getValue()), designDocumentUpdateCompletedMessage.getTrace().toHeaders()));
    }
}
