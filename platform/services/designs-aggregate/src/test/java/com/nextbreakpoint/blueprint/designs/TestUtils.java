package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.core.model.messaging.Message;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.test.PayloadUtils;
import com.nextbreakpoint.blueprint.designs.model.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {
    private TestUtils() {}

    @NotNull
    public static String createRenderKey(TileRenderRequested event) {
        return String.format("%s/%d/%04d%04d.png", event.getDesignId().toString(), event.getLevel(), event.getRow(), event.getCol());
    }

    @NotNull
    public static String createRenderKey(TileRenderCompleted event) {
        return String.format("%s/%d/%04d%04d.png", event.getDesignId().toString(), event.getLevel(), event.getRow(), event.getCol());
    }

    public static int totalTilesByLevels(int levels) {
        return IntStream.range(0, levels).map(TestUtils::totalTileByLevel).sum();
    }

    public static int totalTileByLevel(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    @NotNull
    public static List<Level> convertToTilesList(Map<Integer, Level> tiles) {
        return tiles.values().stream()
                .sorted(Comparator.comparing(Level::getLevel))
                .collect(Collectors.toList());
    }

    @NotNull
    public static Map<Integer, Level> createTilesMap(int levels) {
        return IntStream.range(0, levels)
                .mapToObj(level -> new Level(level, totalTileByLevel(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Level::getLevel, Function.identity()));
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
    public static List<Level> getTiles(int levels, float completePercentage) {
        return IntStream.range(0, levels)
                .mapToObj(level -> makeTiles(level, completePercentage))
                .collect(Collectors.toList());
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

    @NotNull
    private static Level makeTiles(int level, float completePercentage) {
        final int requested = (int) Math.rint(Math.pow(2, level * 2));

        final int completedCount = (int) Math.rint(completePercentage * requested);

        final Set<Integer> completed = TestUtils.generateTiles(level)
                .stream()
                .limit(completedCount)
                .map(tile -> tile.getCol() << 16 | tile.getRow())
                .collect(Collectors.toSet());

        return new Level(level, requested, completed, new HashSet<>());
    }
}
