package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateCompletedOutputMapper;
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

    private final DesignDocumentUpdateCompletedOutputMapper designDocumentUpdateCompletedMapper = new DesignDocumentUpdateCompletedOutputMapper(MESSAGE_SOURCE);
    private final DesignDocumentDeleteCompletedOutputMapper designDocumentDeleteCompletedMapper = new DesignDocumentDeleteCompletedOutputMapper(MESSAGE_SOURCE);

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
        var designDocumentUpdateCompleted1 = DesignDocumentUpdateCompleted.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withRevision(REVISION_0)
                .build();

        var designDocumentUpdateCompleted2 = DesignDocumentUpdateCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_2)
                .withRevision(REVISION_1)
                .build();

        final OutputMessage designDocumentUpdateCompletedMessage1 = designDocumentUpdateCompletedMapper.transform(designDocumentUpdateCompleted1);
        final OutputMessage designDocumentUpdateCompletedMessage2 = designDocumentUpdateCompletedMapper.transform(designDocumentUpdateCompleted2);

        final List<OutputMessage> designDocumentUpdateCompletedMessages = List.of(
                designDocumentUpdateCompletedMessage1,
                designDocumentUpdateCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentUpdateCompletedEvent(designDocumentUpdateCompletedMessages);
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent() {
        var designDocumentUpdateCompleted1 = DesignDocumentUpdateCompleted.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withRevision(REVISION_0)
                .build();

        var designDocumentUpdateCompleted2 = DesignDocumentUpdateCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_2)
                .withRevision(REVISION_1)
                .build();

        final OutputMessage designDocumentUpdateCompletedMessage1 = designDocumentUpdateCompletedMapper.transform(designDocumentUpdateCompleted1);
        final OutputMessage designDocumentUpdateCompletedMessage2 = designDocumentUpdateCompletedMapper.transform(designDocumentUpdateCompleted2);

        final List<OutputMessage> designDocumentUpdateCompletedMessages = List.of(
                designDocumentUpdateCompletedMessage1,
                designDocumentUpdateCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent(designDocumentUpdateCompletedMessages, DESIGN_ID_1);
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent() {
        var designDocumentDeleteCompleted1 = DesignDocumentDeleteCompleted.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withRevision(REVISION_0)
                .build();

        var designDocumentDeleteCompleted2 = DesignDocumentDeleteCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_2)
                .withRevision(REVISION_1)
                .build();

        final OutputMessage designDocumentDeleteCompletedMessage1 = designDocumentDeleteCompletedMapper.transform(designDocumentDeleteCompleted1);
        final OutputMessage designDocumentDeleteCompletedMessage2 = designDocumentDeleteCompletedMapper.transform(designDocumentDeleteCompleted2);

        final List<OutputMessage> designDocumentDeleteCompletedMessages = List.of(
                designDocumentDeleteCompletedMessage1,
                designDocumentDeleteCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingADesignDocumentDeleteCompletedEvent(designDocumentDeleteCompletedMessages);
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent() {
        var designDocumentDeleteCompleted1 = DesignDocumentDeleteCompleted.builder()
                .withDesignId(DESIGN_ID_1)
                .withCommandId(COMMAND_ID_1)
                .withRevision(REVISION_0)
                .build();

        var designDocumentDeleteCompleted2 = DesignDocumentDeleteCompleted.builder()
                .withDesignId(DESIGN_ID_2)
                .withCommandId(COMMAND_ID_2)
                .withRevision(REVISION_1)
                .build();

        final OutputMessage designDocumentDeleteCompletedMessage1 = designDocumentDeleteCompletedMapper.transform(designDocumentDeleteCompleted1);
        final OutputMessage designDocumentDeleteCompletedMessage2 = designDocumentDeleteCompletedMapper.transform(designDocumentDeleteCompleted2);

        final List<OutputMessage> designDocumentDeleteCompletedMessages = List.of(
                designDocumentDeleteCompletedMessage1,
                designDocumentDeleteCompletedMessage2
        );

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent(designDocumentDeleteCompletedMessages, DESIGN_ID_1);
    }
}
