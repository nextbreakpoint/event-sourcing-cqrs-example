package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAbortRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAbortRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignInsertRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class IntegrationTests {
    private static TestCases testCases = new TestCases("IntegrationTests");

    @BeforeAll
    public static void before() throws IOException, InterruptedException {
        testCases.before();
    }

    @AfterAll
    public static void after() throws IOException, InterruptedException {
        testCases.after();
    }

    @Nested
    @Tag("slow")
    @Tag("integration")
    @DisplayName("Verify behaviour of designs-tile-renderer service")
    public class VerifyServiceApi {
        @Test
        @DisplayName("Should start rendering an image after receiving a TileRenderRequested event")
        public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage() {
            final UUID designId = UUID.randomUUID();

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

            final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(Uuids.timeBased(), designId, 0, TestConstants.JSON_1, Checksum.of(TestConstants.JSON_1), 0, 0, 0);

            final OutputMessage tileRenderRequestedMessage1 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createBucketKey).transform(tileRenderRequested1);

            final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(Uuids.timeBased(), designId, 1, TestConstants.JSON_2, Checksum.of(TestConstants.JSON_2), 1, 1, 2);

            final OutputMessage tileRenderRequestedMessage2 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createBucketKey).transform(tileRenderRequested2);

            testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(designInsertRequestedMessage, List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2));
        }

        @Test
        @Disabled
        @DisplayName("Should abort rendering a pending images after receiving a DesignAbortRequested event")
        public void shouldAbortRenderingImagesWhenReceivingADesignAbortRequestedMessage() {
            final UUID designId = UUID.fromString("ea55b659-a6df-409c-9c5b-85ea067f0f38");

            final DesignInsertRequested designInsertRequested = new DesignInsertRequested(Uuids.timeBased(), designId, TestConstants.JSON_1, TestConstants.LEVELS);

            final OutputMessage designInsertRequestedMessage = new DesignInsertRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designInsertRequested);

            final DesignAbortRequested designAbortRequested1 = new DesignAbortRequested(Uuids.timeBased(), designId, Checksum.of(TestConstants.JSON_1));

            final OutputMessage designAbortRequestedMessage1 = new DesignAbortRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAbortRequested1);

//            final DesignAbortRequested designAbortRequested2 = new DesignAbortRequested(Uuids.timeBased(), designId, Checksum.of(TestConstants.JSON_2));
//
//            final OutputMessage designAbortRequestedMessage2 = new DesignAbortRequestedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAbortRequested2);

            testCases.shouldAbortRenderingImagesWhenReceivingADesignAbortRequestedMessage(designInsertRequestedMessage, designAbortRequestedMessage1);
        }
    }
}