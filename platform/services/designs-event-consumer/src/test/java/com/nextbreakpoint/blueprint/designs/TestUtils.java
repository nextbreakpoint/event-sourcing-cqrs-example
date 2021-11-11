package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.json.Json;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {
    public static int totalTilesByLevels(int levels) {
        return IntStream.range(0, levels).map(TestUtils::totalTileByLevel).sum();
    }

    public static int totalTileByLevel(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    @NotNull
    public static List<Tiles> convertToTilesList(Map<Integer, Tiles> tiles) {
        return tiles.values().stream()
                .sorted(Comparator.comparing(Tiles::getLevel))
                .collect(Collectors.toList());
    }

    @NotNull
    public static Map<Integer, Tiles> createTilesMap(int levels) {
        return IntStream.range(0, levels)
                .mapToObj(level -> new Tiles(level, totalTileByLevel(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    @NotNull
    public static List<TileRenderRequested> extractTileRenderRequestedEvents(List<InputMessage> messages, String checksum) {
        return messages.stream()
                .map(message -> Json.decodeValue(message.getValue().getData(), TileRenderRequested.class))
                .filter(event -> event.getChecksum().equals(checksum))
                .collect(Collectors.toList());
    }

    @NotNull
    public static Set<UUID> extractUuids(List<Row> rows) {
        return rows.stream()
                .map(row -> row.getString("MESSAGE_KEY"))
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }
}
