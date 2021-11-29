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
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdateCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileAggregateUpdateCompletedOutputMapper;
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
@DisplayName("Verify contract between designs-event-consumer and designs-notification-dispatcher")
@Provider("designs-event-consumer")
@Consumer("designs-notification-dispatcher")
@PactBroker
public class VerifyNotificationDispatcherPact {
    public VerifyNotificationDispatcherPact() {
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

    @PactVerifyProvider("design aggregate update completed for design 00000000-0000-0000-0000-000000000001")
    public String produceDesignAggregateUpdateCompleted1() {
        return produceDesignAggregateUpdateCompleted(new UUID(0L, 1L), TestConstants.JSON_1, TestConstants.CHECKSUM_1, "CREATED");
    }

    @PactVerifyProvider("design aggregate update completed for design 00000000-0000-0000-0000-000000000002")
    public String produceDesignAggregateUpdateCompleted2() {
        return produceDesignAggregateUpdateCompleted(new UUID(0L, 2L), TestConstants.JSON_2, TestConstants.CHECKSUM_2, "UPDATED");
    }

    @PactVerifyProvider("tile aggregate update completed for design 00000000-0000-0000-0000-000000000001")
    public String produceTileAggregateUpdateCompleted1() {
        return produceTileAggregateUpdateCompleted(new UUID(0L, 1L));
    }

    @PactVerifyProvider("tile aggregate update completed for design 00000000-0000-0000-0000-000000000002")
    public String produceTileAggregateUpdateCompleted2() {
        return produceTileAggregateUpdateCompleted(new UUID(0L, 2L));
    }

    private String produceDesignAggregateUpdateCompleted(UUID uuid, String data, String checksum, String status) {
        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted = new DesignAggregateUpdateCompleted(Uuids.timeBased(), uuid, 0, data, checksum, TestConstants.LEVELS, status);

        final OutputMessage designAggregateUpdateCompletedMessage = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted);

        return Json.encode(new KafkaRecord(designAggregateUpdateCompletedMessage.getKey(), PayloadUtils.payloadToMap(designAggregateUpdateCompletedMessage.getValue())));
    }

    private String produceTileAggregateUpdateCompleted(UUID uuid) {
        final TileAggregateUpdateCompleted designAggregateUpdateCompleted = new TileAggregateUpdateCompleted(Uuids.timeBased(), uuid, 0);

        final OutputMessage designAggregateUpdateCompletedMessage = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted);

        return Json.encode(new KafkaRecord(designAggregateUpdateCompletedMessage.getKey(), PayloadUtils.payloadToMap(designAggregateUpdateCompletedMessage.getValue())));
    }
}
