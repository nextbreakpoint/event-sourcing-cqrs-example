package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdateCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileAggregateUpdateCompletedOutputMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of designs-notification-dispatcher service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases();

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        System.setProperty("http.port", "30123");

        testCases.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        testCases.after();
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted1 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0, TestConstants.JSON_1, TestConstants.CHECKSUM_1, TestConstants.LEVELS, "CREATED");

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted2 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0, TestConstants.JSON_2, TestConstants.CHECKSUM_2, TestConstants.LEVELS, "UPDATED");

        final OutputMessage designAggregateUpdateCompletedMessage1 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted1 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0, TestConstants.JSON_1, TestConstants.CHECKSUM_1, TestConstants.LEVELS, "CREATED");

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted2 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0, TestConstants.JSON_2, TestConstants.CHECKSUM_2, TestConstants.LEVELS, "UPDATED");

        final OutputMessage designAggregateUpdateCompletedMessage1 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a TileAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingATileAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted1 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0);

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted2 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designAggregateUpdateCompletedMessage1 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a TileAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnTileAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted1 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0);

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted2 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designAggregateUpdateCompletedMessage1 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }
}
