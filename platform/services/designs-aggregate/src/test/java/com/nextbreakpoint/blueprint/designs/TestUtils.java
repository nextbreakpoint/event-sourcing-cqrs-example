package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.messaging.Message;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
    public static List<TileRenderRequested> extractTileRenderRequestedEvents(List<InputMessage> messages, String checksum) {
        return messages.stream()
                .map(message -> Json.decodeValue(message.getValue().getData(), TileRenderRequested.class))
                .filter(event -> event.getChecksum().equals(checksum))
                .collect(Collectors.toList());
    }

    @NotNull
    public static OutputMessage toOutputMessage(Message message) {
        final KafkaRecord kafkaRecord = Json.decodeValue(message.contentsAsString(), KafkaRecord.class);
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
        final int limit = (int) Math.rint(completePercentage * total);

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
}
