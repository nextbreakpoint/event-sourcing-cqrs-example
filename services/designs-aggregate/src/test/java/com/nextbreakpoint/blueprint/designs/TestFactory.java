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
import com.nextbreakpoint.blueprint.common.test.MessageUtils;
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
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILES_RENDERED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_COMPLETED;
import static com.nextbreakpoint.blueprint.designs.TestConstants.TILE_RENDER_REQUESTED;

public class TestFactory {
    private TestFactory() {}

    @NotNull
    public static OutputMessage<TilesRendered> createOutputMessage(UUID messageId, TilesRendered tilesRendered) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, tilesRendered.getDesignId().toString(), TILES_RENDERED, messageId, tilesRendered
        );
    }

    @NotNull
    public static InputMessage<DesignAggregateUpdated> of(UUID messageId, String messageToken, LocalDateTime messageTime, DesignAggregateUpdated designAggregateUpdated) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designAggregateUpdated.getDesignId().toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<DesignDocumentUpdateRequested> createInputMessage(UUID messageId, DesignDocumentUpdateRequested designDocumentUpdateRequested) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, designDocumentUpdateRequested.getDesignId().toString(), DESIGN_DOCUMENT_UPDATE_REQUESTED, messageId, designDocumentUpdateRequested
        );
    }

    @NotNull
    public static OutputMessage<DesignDocumentDeleteRequested> createOutputMessage(UUID messageId, DesignDocumentDeleteRequested designDocumentDeleteRequested) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, designDocumentDeleteRequested.getDesignId().toString(), DESIGN_DOCUMENT_DELETE_REQUESTED, messageId, designDocumentDeleteRequested
        );
    }

    @NotNull
    public static InputMessage<DesignInsertRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignInsertRequested designInsertRequested) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designInsertRequested.getDesignId().toString(), DESIGN_INSERT_REQUESTED, messageId, designInsertRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignUpdateRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignUpdateRequested designUpdateRequested) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designUpdateRequested.getDesignId().toString(), DESIGN_UPDATE_REQUESTED, messageId, designUpdateRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static InputMessage<DesignDeleteRequested> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, DesignDeleteRequested designDeleteRequested) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, designDeleteRequested.getDesignId().toString(), DESIGN_DELETE_REQUESTED, messageId, designDeleteRequested, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<DesignAggregateUpdated> createOutputMessage(UUID messageId, DesignAggregateUpdated designAggregateUpdated) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, designAggregateUpdated.getDesignId().toString(), DESIGN_AGGREGATE_UPDATED, messageId, designAggregateUpdated
        );
    }

    @NotNull
    public static InputMessage<TileRenderCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, tileRenderCompleted.getDesignId().toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, tileRenderCompleted.getDesignId().toString(), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
        );
    }

    @NotNull
    public static InputMessage<TileRenderCompleted> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TileRenderCompleted tileRenderCompleted, Function<TileRenderCompleted, String> keyMapper) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, keyMapper.apply(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted, messageToken, messageTime
        );
    }

    @NotNull
    public static OutputMessage<TileRenderCompleted> createOutputMessage(UUID messageId, TileRenderCompleted tileRenderCompleted, Function<TileRenderCompleted, String> keyMapper) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, keyMapper.apply(tileRenderCompleted), TILE_RENDER_COMPLETED, messageId, tileRenderCompleted
        );
    }

    @NotNull
    public static OutputMessage<TileRenderRequested> createOutputMessage(UUID messageId, TileRenderRequested tileRenderRequested) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, tileRenderRequested.getDesignId().toString(), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
        );
    }

    @NotNull
    public static OutputMessage<TileRenderRequested> createOutputMessage(UUID messageId, TileRenderRequested tileRenderRequested, Function<TileRenderRequested, String> keyMapper) {
        return MessageUtils.createOutputMessage(
                MESSAGE_SOURCE, keyMapper.apply(tileRenderRequested), TILE_RENDER_REQUESTED, messageId, tileRenderRequested
        );
    }

    @NotNull
    public static InputMessage<TilesRendered> createInputMessage(UUID messageId, String messageToken, LocalDateTime messageTime, TilesRendered tilesRendered) {
        return MessageUtils.createInputMessage(
                MESSAGE_SOURCE, tilesRendered.getDesignId().toString(), TILES_RENDERED, messageId, tilesRendered, messageToken, messageTime
        );
    }
}
