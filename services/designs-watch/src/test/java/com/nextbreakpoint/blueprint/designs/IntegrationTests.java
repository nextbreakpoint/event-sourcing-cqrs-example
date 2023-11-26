package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-watch service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases();

    @BeforeAll
    public static void before() {
        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @BeforeEach
    public void beforeEach() {
        testCases.getSteps().reset();
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent() {
        var designDocumentUpdateCompleted1 = DesignDocumentUpdateCompleted.newBuilder()
                .setDesignId(DESIGN_ID_1)
                .setCommandId(COMMAND_ID_1)
                .setRevision(REVISION_0)
                .build();

        var designDocumentUpdateCompleted2 = DesignDocumentUpdateCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setRevision(REVISION_1)
                .build();

        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage1 = MessageFactory.<DesignDocumentUpdateCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentUpdateCompleted1.getDesignId().toString(), designDocumentUpdateCompleted1);
        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage2 = MessageFactory.<DesignDocumentUpdateCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentUpdateCompleted2.getDesignId().toString(), designDocumentUpdateCompleted2);

        final List<OutputMessage<DesignDocumentUpdateCompleted>> designDocumentUpdateCompletedMessages = List.of(
                designDocumentUpdateCompletedMessage1,
                designDocumentUpdateCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(designDocumentUpdateCompletedMessages);
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent() {
        var designDocumentUpdateCompleted1 = DesignDocumentUpdateCompleted.newBuilder()
                .setDesignId(DESIGN_ID_1)
                .setCommandId(COMMAND_ID_1)
                .setRevision(REVISION_0)
                .build();

        var designDocumentUpdateCompleted2 = DesignDocumentUpdateCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setRevision(REVISION_1)
                .build();

        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage1 = MessageFactory.<DesignDocumentUpdateCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentUpdateCompleted1.getDesignId().toString(), designDocumentUpdateCompleted1);
        final OutputMessage<DesignDocumentUpdateCompleted> designDocumentUpdateCompletedMessage2 = MessageFactory.<DesignDocumentUpdateCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentUpdateCompleted2.getDesignId().toString(), designDocumentUpdateCompleted2);

        final List<OutputMessage<DesignDocumentUpdateCompleted>> designDocumentUpdateCompletedMessages = List.of(
                designDocumentUpdateCompletedMessage1,
                designDocumentUpdateCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(designDocumentUpdateCompletedMessages, DESIGN_ID_1);
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent() {
        var designDocumentDeleteCompleted1 = DesignDocumentDeleteCompleted.newBuilder()
                .setDesignId(DESIGN_ID_1)
                .setCommandId(COMMAND_ID_1)
                .setRevision(REVISION_0)
                .build();

        var designDocumentDeleteCompleted2 = DesignDocumentDeleteCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setRevision(REVISION_1)
                .build();

        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage1 = MessageFactory.<DesignDocumentDeleteCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentDeleteCompleted1.getDesignId().toString(), designDocumentDeleteCompleted1);
        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage2 = MessageFactory.<DesignDocumentDeleteCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentDeleteCompleted2.getDesignId().toString(), designDocumentDeleteCompleted2);

        final List<OutputMessage<DesignDocumentDeleteCompleted>> designDocumentDeleteCompletedMessages = List.of(
                designDocumentDeleteCompletedMessage1,
                designDocumentDeleteCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent(designDocumentDeleteCompletedMessages);
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent() {
        var designDocumentDeleteCompleted1 = DesignDocumentDeleteCompleted.newBuilder()
                .setDesignId(DESIGN_ID_1)
                .setCommandId(COMMAND_ID_1)
                .setRevision(REVISION_0)
                .build();

        var designDocumentDeleteCompleted2 = DesignDocumentDeleteCompleted.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setRevision(REVISION_1)
                .build();

        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage1 = MessageFactory.<DesignDocumentDeleteCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentDeleteCompleted1.getDesignId().toString(), designDocumentDeleteCompleted1);
        final OutputMessage<DesignDocumentDeleteCompleted> designDocumentDeleteCompletedMessage2 = MessageFactory.<DesignDocumentDeleteCompleted>of(MESSAGE_SOURCE).createOutputMessage(designDocumentDeleteCompleted2.getDesignId().toString(), designDocumentDeleteCompleted2);

        final List<OutputMessage<DesignDocumentDeleteCompleted>> designDocumentDeleteCompletedMessages = List.of(
                designDocumentDeleteCompletedMessage1,
                designDocumentDeleteCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent(designDocumentDeleteCompletedMessages, DESIGN_ID_1);
    }
}
