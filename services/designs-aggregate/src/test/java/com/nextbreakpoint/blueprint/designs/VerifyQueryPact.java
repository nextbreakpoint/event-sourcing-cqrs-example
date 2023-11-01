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
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_5;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;

@Tag("docker")
@Tag("pact-verify")
@DisplayName("Verify contract between designs-aggregate and designs-query")
@Provider("designs-aggregate")
@Consumer("designs-query")
@PactBroker
public class VerifyQueryPact {
    public VerifyQueryPact() {
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

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000004 with 0% tiles completed and not published")
    public String produceDesignDocumentUpdateRequested1() {
        return produceDesignDocumentUpdateRequested(DESIGN_ID_4, COMMAND_ID_1, DATA_1, Checksum.of(DATA_1), "CREATED", 0.0f, REVISION_0, USER_ID_1, false, LEVELS_DRAFT);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000004 with 50% tiles completed and not published")
    public String produceDesignDocumentUpdateRequested2() {
        return produceDesignDocumentUpdateRequested(DESIGN_ID_4, COMMAND_ID_2, DATA_2, Checksum.of(DATA_2), "UPDATED", 0.5f, REVISION_1, USER_ID_1, false, LEVELS_DRAFT);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000004 with 100% tiles completed and not published")
    public String produceDesignDocumentUpdateRequested3() {
        return produceDesignDocumentUpdateRequested(DESIGN_ID_4, COMMAND_ID_3, DATA_2, Checksum.of(DATA_2), "UPDATED", 1.0f, REVISION_2, USER_ID_1, false, LEVELS_DRAFT);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000004 with 100% tiles completed and published")
    public String produceDesignDocumentUpdateRequested4() {
        return produceDesignDocumentUpdateRequested(DESIGN_ID_4, COMMAND_ID_4, DATA_2, Checksum.of(DATA_2), "UPDATED", 1.0f, REVISION_3, USER_ID_1, true, LEVELS_READY);
    }

    @PactVerifyProvider("design document update requested for design 00000000-0000-0000-0000-000000000005 and 100% tiles completed and published")
    public String produceDesignDocumentUpdateRequested5() {
        return produceDesignDocumentUpdateRequested(DESIGN_ID_5, COMMAND_ID_1, DATA_1, Checksum.of(DATA_1), "UPDATED", 1.0f, REVISION_0, USER_ID_2, true, LEVELS_READY);
    }

    @PactVerifyProvider("design document delete requested for design 00000000-0000-0000-0000-000000000005")
    public String produceDesignDocumentDeleteRequested() {
        return produceDesignDocumentDeleteRequested(DESIGN_ID_5, COMMAND_ID_2, REVISION_0);
    }

    private String produceDesignDocumentUpdateRequested(UUID designId, UUID commandId, String data, String checksum, String status, float completePercentage, String revision, UUID userId, boolean published, int levels) {
        final TilesBitmap bitmap = TestUtils.createBitmap(levels, completePercentage);

        final List<Tiles> tiles = IntStream.range(0, 8).mapToObj(bitmap::toTiles).collect(Collectors.toList());

        final DesignDocumentUpdateRequested designDocumentUpdateRequested = new DesignDocumentUpdateRequested(designId, commandId, userId, revision, checksum, data, status, published, levels, tiles, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));

        final OutputMessage designDocumentUpdateRequestedMessage = new DesignDocumentUpdateRequestedOutputMapper(MESSAGE_SOURCE).transform(designDocumentUpdateRequested);

        return Json.encodeValue(new KafkaRecord(designDocumentUpdateRequestedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentUpdateRequestedMessage.getValue())));
    }

    private String produceDesignDocumentDeleteRequested(UUID designId, UUID commandId, String revision) {
        final DesignDocumentDeleteRequested designDocumentDeleteRequested = new DesignDocumentDeleteRequested(designId, commandId, revision);

        final OutputMessage designDocumentDeleteRequestedMessage = new DesignDocumentDeleteRequestedOutputMapper(MESSAGE_SOURCE).transform(designDocumentDeleteRequested);

        return Json.encodeValue(new KafkaRecord(designDocumentDeleteRequestedMessage.getKey(), PayloadUtils.payloadToMap(designDocumentDeleteRequestedMessage.getValue())));
    }
}
