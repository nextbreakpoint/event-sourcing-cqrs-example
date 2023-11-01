package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.KafkaRecord;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.Tile;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static String createRenderKey(TileRenderRequested event) {
        return String.format("%s/%s/%d/%04d%04d.png", event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
    }

    @NotNull
    public static String createRenderKey(TileRenderCompleted event) {
        return String.format("%s/%s/%d/%04d%04d.png", event.getDesignId(), event.getCommandId(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static int totalTilesByLevels(int levels) {
        return IntStream.range(0, levels).map(TestUtils::totalTileByLevel).sum();
    }

    public static int totalTileByLevel(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    @NotNull
    public static List<TileRenderRequested> extractTileRenderRequestedEvents(List<InputMessage> messages, Predicate<TileRenderRequested> predicate) {
        return messages.stream()
                .map(TestUtils::extractTileRenderRequestedEvent)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @NotNull
    public static TileRenderRequested extractTileRenderRequestedEvent(InputMessage message) {
        return Json.decodeValue(message.getValue().getData(), TileRenderRequested.class);
    }

    @NotNull
    public static DesignAggregateUpdated extractDesignAggregateUpdatedEvent(InputMessage message) {
        return Json.decodeValue(message.getValue().getData(), DesignAggregateUpdated.class);
    }

    @NotNull
    public static DesignDocumentUpdateRequested extractDesignDocumentUpdateRequestedEvent(InputMessage inputMessage) {
        return Json.decodeValue(inputMessage.getValue().getData(), DesignDocumentUpdateRequested.class);
    }

    @NotNull
    public static DesignDocumentDeleteRequested extractDesignDocumentDeleteRequestedEvent(InputMessage inputMessage) {
        return Json.decodeValue(inputMessage.getValue().getData(), DesignDocumentDeleteRequested.class);
    }

    @NotNull
    public static OutputMessage toOutputMessage(V4Interaction.AsynchronousMessage message) {
        final KafkaRecord kafkaRecord = Json.decodeValue(message.getContents().getContents().valueAsString(), KafkaRecord.class);
        return OutputMessage.from(kafkaRecord.getKey(), PayloadUtils.mapToPayload(kafkaRecord.getValue()));
    }

    @NotNull
    public static TilesBitmap createBitmap(int levels, float completePercentage) {
        TilesBitmap bitmap = TilesBitmap.empty();

        IntStream.range(0, levels)
                .forEach(level -> TestUtils.makeLevel(bitmap, level, completePercentage));

        return bitmap;
    }

    private static void makeLevel(TilesBitmap bitmap, int level, float completePercentage) {
        final int total = (int) Math.rint(Math.pow(2, level * 2));
        final int limit = (int) Math.ceil(completePercentage * total);

        TestUtils.generateTiles(level)
                .stream()
                .limit(limit)
                .forEach(tile -> bitmap.putTile(tile.getLevel(), tile.getRow(), tile.getCol()));
    }

    @NotNull
    private static List<Tile> generateTiles(int level) {
        final int size = (int) Math.rint(Math.pow(2, level));
        return IntStream.range(0, size)
                .boxed()
                .flatMap(row ->
                        IntStream.range(0, size)
                                .boxed()
                                .map(col -> new com.nextbreakpoint.blueprint.common.core.Tile(level, row, col))
                )
                .collect(Collectors.toList());
    }

    @NotNull
    public static InputMessage createInputMessage(String messageKey, String messageType, String messageToken, LocalDateTime messageTime, Object event) {
        final Payload payload = Payload.builder()
                .withUuid(UUID.randomUUID())
                .withData(Json.encodeValue(event))
                .withType(messageType)
                .withSource(MESSAGE_SOURCE)
                .build();

        return InputMessage.builder()
                .key(messageKey)
                .value(payload)
                .token(messageToken)
                .timestamp(messageTime.toInstant(ZoneOffset.UTC).toEpochMilli())
                .build();
    }
}
