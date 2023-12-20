package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.V4Interaction;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Tile;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.KafkaRecord;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import org.apache.avro.specific.SpecificRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public static List<TileRenderRequested> extractTileRenderRequestedEvents(List<InputMessage<Object>> messages, Predicate<TileRenderRequested> predicate) {
        return messages.stream()
                .map(TestUtils::extractTileRenderRequestedEvent)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @NotNull
    public static TileRenderRequested extractTileRenderRequestedEvent(InputMessage<Object> message) {
        return (TileRenderRequested) message.getValue().getData();
    }

    @NotNull
    public static DesignAggregateUpdated extractDesignAggregateUpdatedEvent(InputMessage<Object> message) {
        return (DesignAggregateUpdated) message.getValue().getData();
    }

    @NotNull
    public static DesignDocumentUpdateRequested extractDesignDocumentUpdateRequestedEvent(InputMessage<Object> message) {
        return (DesignDocumentUpdateRequested) message.getValue().getData();
    }

    @NotNull
    public static DesignDocumentDeleteRequested extractDesignDocumentDeleteRequestedEvent(InputMessage<Object> message) {
        return (DesignDocumentDeleteRequested) message.getValue().getData();
    }

    @NotNull
    public static <T extends SpecificRecord> OutputMessage<T> toOutputMessage(V4Interaction.AsynchronousMessage message, Class<T> clazz) {
        final String json = message.getContents().getContents().valueAsString();
        final KafkaRecord kafkaRecord = Json.decodeValue(json, KafkaRecord.class);

        return OutputMessage.<T>builder()
                .withKey(kafkaRecord.getKey())
                .withValue(PayloadUtils.mapToPayload(kafkaRecord.getValue(), clazz))
                .build();
    }

    @NotNull
    public static Bitmap createBitmap(int levels, float completePercentage) {
        Bitmap bitmap = Bitmap.empty();

        IntStream.range(0, levels)
                .forEach(level -> TestUtils.makeLevel(bitmap, level, completePercentage));

        return bitmap;
    }

    private static void makeLevel(Bitmap bitmap, int level, float completePercentage) {
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
                                .map(col -> new Tile(level, row, col))
                )
                .collect(Collectors.toList());
    }
}
