package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_AGGREGATE_UPDATED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_DELETE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DOCUMENT_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_REQUESTED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILES_RENDERED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;

public class TestFactory {
    private TestFactory() {}

    @NotNull
    public static OutputMessage<TilesRendered> createOutputMessage(UUID messageId, TilesRendered tilesRendered) {
        return TestUtils.createOutputMessage(
                tilesRendered.getDesignId().toString(), TILES_RENDERED, messageId, tilesRendered
        );
    }

    @NotNull
    public static InputMessage<DesignAggregateUpdated> of(UUID messageId, String messageToken, LocalDateTime messageTime, DesignAggregateUpdated designAggregateUpdated) {
        return TestUtils.createInputMessage(
                designAggregateUpdated.getDesignId().toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<DesignDocumentUpdateRequested> createInputMessage(UUID messageId, DesignDocumentUpdateRequested designDocumentUpdateRequested) {
        return TestUtils.createOutputMessage(
                designDocumentUpdateRequested.getDesignId().toString(), DESIGN_DOCUMENT_UPDATE_REQUESTED, messageId, designDocumentUpdateRequested
        );
    }

    @NotNull
    public static OutputMessage<DesignDocumentDeleteRequested> createOutputMessage(UUID messageId, DesignDocumentDeleteRequested designDocumentDeleteRequested) {
        return TestUtils.createOutputMessage(
                designDocumentDeleteRequested.getDesignId().toString(), DESIGN_DOCUMENT_DELETE_REQUESTED, messageId, designDocumentDeleteRequested
        );
    }

    @NotNull
    public static InputMessage<DesignInsertRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignInsertRequested designInsertRequested) {
        return TestUtils.createInputMessage(
                designInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, messageId, designInsertRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignUpdateRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignUpdateRequested designUpdateRequested) {
        return TestUtils.createInputMessage(
                designUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, messageId, designUpdateRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignDeleteRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDeleteRequested designDeleteRequested) {
        return TestUtils.createInputMessage(
                designDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, messageId, designDeleteRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<DesignAggregateUpdated> createOutputMessage(UUID messageId, DesignAggregateUpdated designAggregateUpdated) {
        return TestUtils.createOutputMessage(
                designAggregateUpdated.getDesignId().toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated
        );
    }

    @NotNull
    public static InputMessage<TileRenderCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted) {
        return TestUtils.createInputMessage(
                tileRenderCompleted.getDesignId().toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted) {
        return TestUtils.createOutputMessage(
                tileRenderCompleted.getDesignId().toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
        );
    }

    @NotNull
    public static InputMessage<TileRenderCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted, Function<TileRenderCompleted, String> keyMapper) {
        return TestUtils.createInputMessage(
                keyMapper.apply(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted, Function<TileRenderCompleted, String> keyMapper) {
        return TestUtils.createOutputMessage(
                keyMapper.apply(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
        );
    }

    @NotNull
    public static OutputMessage<TileRenderRequested> createOutputMessage(UUID messageId, TileRenderRequested tileRenderRequested) {
        return TestUtils.createOutputMessage(
                tileRenderRequested.getDesignId().toString(), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
        );
    }

    @NotNull
    public static OutputMessage<TileRenderRequested> createOutputMessage(UUID messageId, TileRenderRequested tileRenderRequested, Function<TileRenderRequested, String> keyMapper) {
        return TestUtils.createOutputMessage(
                keyMapper.apply(tileRenderRequested), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
        );
    }

    @NotNull
    public static InputMessage<TilesRendered> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TilesRendered tilesRendered) {
        return TestUtils.createInputMessage(
                tilesRendered.getDesignId().toString(), TILES_RENDERED, messageId, tilesRendered, messageToken, messageTime
        );
    }
}
