package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateCompletedOutputMapper;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of designs-notify service")
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

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted1 = new DesignDocumentUpdateCompleted(Uuids.timeBased(), designId1, 0);

        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted2 = new DesignDocumentUpdateCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designDocumentUpdateCompletedMessage1 = new DesignDocumentUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentUpdateCompleted1);

        final OutputMessage designDocumentUpdateCompletedMessage2 = new DesignDocumentUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentUpdateCompleted2);

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designDocumentUpdateCompletedMessage1, designDocumentUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted1 = new DesignDocumentUpdateCompleted(Uuids.timeBased(), designId1, 0);

        final DesignDocumentUpdateCompleted designDocumentUpdateCompleted2 = new DesignDocumentUpdateCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designDocumentUpdateCompletedMessage1 = new DesignDocumentUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentUpdateCompleted1);

        final OutputMessage designDocumentUpdateCompletedMessage2 = new DesignDocumentUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentUpdateCompleted2);

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designDocumentUpdateCompletedMessage1, designDocumentUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignAggregateDeleteCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignAggregateDeleteCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignDocumentDeleteCompleted designDocumentDeleteCompleted1 = new DesignDocumentDeleteCompleted(Uuids.timeBased(), designId1, 0);

        final DesignDocumentDeleteCompleted designDocumentDeleteCompleted2 = new DesignDocumentDeleteCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designDocumentDeleteCompletedMessage1 = new DesignDocumentDeleteCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentDeleteCompleted1);

        final OutputMessage designDocumentDeleteCompletedMessage2 = new DesignDocumentDeleteCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentDeleteCompleted2);

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designDocumentDeleteCompletedMessage1, designDocumentDeleteCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignDocumentDeleteCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignDocumentDeleteCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignDocumentDeleteCompleted designDocumentDeleteCompleted1 = new DesignDocumentDeleteCompleted(Uuids.timeBased(), designId1, 0);

        final DesignDocumentDeleteCompleted designDocumentDeleteCompleted2 = new DesignDocumentDeleteCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designDocumentDeleteCompletedMessage1 = new DesignDocumentDeleteCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentDeleteCompleted1);

        final OutputMessage designDocumentDeleteCompletedMessage2 = new DesignDocumentDeleteCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designDocumentDeleteCompleted2);

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designDocumentDeleteCompletedMessage1, designDocumentDeleteCompletedMessage2));
    }
}
