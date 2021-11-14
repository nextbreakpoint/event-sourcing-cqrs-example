package com.nextbreakpoint.blueprint.designs.common;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.designs.model.DesignAccumulator;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.json.Json;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignAggregate {
    private Tiles TILES_EMPTY = new Tiles(0, 0, Collections.emptySet(), Collections.emptySet());

    public Optional<DesignAccumulator> mergeEvents(DesignAccumulator accumulator, List<Row> rows) {
        if (accumulator != null) {
            return Optional.of(rows.stream().map(this::convertRowToAccumulator).reduce(accumulator, this::mergeElement)).filter(a -> a.getStatus() != null);
        } else {
            return rows.stream().map(this::convertRowToAccumulator).reduce(this::mergeElement).filter(a -> a.getStatus() != null);
        }
    }

    private DesignAccumulator convertRowToAccumulator(Row row) {
        final long offset = row.getLong("MESSAGE_OFFSET");
        final String type = row.getString("MESSAGE_TYPE");
        final String value = row.getString("MESSAGE_VALUE");
        final Instant timestamp = row.getInstant("MESSAGE_TIMESTAMP");
        switch (type) {
            case MessageType.DESIGN_INSERT_REQUESTED: {
                DesignInsertRequested event = Json.decodeValue(value, DesignInsertRequested.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, event.getData(), "CREATED", Checksum.of(event.getData()), event.getLevels(), createLevelsMap(event.getLevels()), new Date(timestamp.toEpochMilli()));
            }
            case MessageType.DESIGN_UPDATE_REQUESTED: {
                DesignUpdateRequested event = Json.decodeValue(value, DesignUpdateRequested.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, event.getData(), "UPDATED", Checksum.of(event.getData()), event.getLevels(), null, new Date(timestamp.toEpochMilli()));
            }
            case MessageType.DESIGN_DELETE_REQUESTED: {
                DesignDeleteRequested event = Json.decodeValue(value, DesignDeleteRequested.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, null, "DELETED", null, 0, null, new Date(timestamp.toEpochMilli()));
            }
            case MessageType.TILE_RENDER_COMPLETED: {
                TileRenderCompleted event = Json.decodeValue(value, TileRenderCompleted.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, null, null, null, 0, createLevelsMap(event), new Date(timestamp.toEpochMilli()));
            }
            default: {
                return new DesignAccumulator(null, null, 0, null, null, null, 0, null, null);
            }
        }
    }

    private Map<Integer, Tiles> createLevelsMap(int levels) {
        return IntStream.range(0, levels)
                .mapToObj(level -> new Tiles(level, getTilesCount(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Map<Integer, Tiles> createLevelsMap(TileRenderCompleted event) {
        return IntStream.of(event.getLevel())
                .mapToObj(level -> new Tiles(level, getTilesCount(level), getCompleted(event, level), getFailed(event, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<Integer> getCompleted(TileRenderCompleted event, int level) {
        return level == event.getLevel() && isCompleted(event) ? Set.of((0xFFFF & event.getRow()) << 16 | (0xFFFF & event.getCol())) : Collections.emptySet();
    }

    private Set<Integer> getFailed(TileRenderCompleted event, int level) {
        return level == event.getLevel() && isCompleted(event) ? Collections.emptySet() : Set.of((0xFFFF & event.getRow()) << 16 | (0xFFFF & event.getCol()));
    }

    private Map<Integer, Tiles> createLevelsMap(DesignAccumulator accumulator, DesignAccumulator element) {
        return IntStream.range(0, accumulator.getLevels())
                .mapToObj(level -> new Tiles(level, getTilesCount(level), mergeCompleted(accumulator, element, level), mergeFailed(accumulator, element, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<Integer> mergeCompleted(DesignAccumulator accumulator, DesignAccumulator element, int level) {
        Set<Integer> combined = new HashSet<>(accumulator.getTiles().get(level).getCompleted());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getCompleted());
        return combined;
    }

    private Set<Integer> mergeFailed(DesignAccumulator accumulator, DesignAccumulator element, int level) {
        Set<Integer> combined = new HashSet<>(accumulator.getTiles().get(level).getFailed());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getFailed());
        return combined;
    }

    private boolean isCompleted(TileRenderCompleted event) {
        return event.getStatus().equalsIgnoreCase("COMPLETED");
    }

    private int getTilesCount(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    private DesignAccumulator mergeElement(DesignAccumulator accumulator, DesignAccumulator element) {
        if (accumulator.getStatus() == null) {
            return accumulator;
        }
        if (element.getStatus() == null && element.getTiles() == null) {
            return accumulator;
        }
        if (element.getStatus() == null) {
            return new DesignAccumulator(accumulator.getEvid(), accumulator.getUuid(), element.getEsid(), accumulator.getJson(), accumulator.getStatus(), accumulator.getChecksum(), accumulator.getLevels(), createLevelsMap(accumulator, element), element.getUpdated());
        }
        if ("DELETED".equals(element.getStatus())) {
            return new DesignAccumulator(element.getEvid(), element.getUuid(), element.getEsid(), accumulator.getJson(), element.getStatus(), accumulator.getChecksum(), accumulator.getLevels(), accumulator.getTiles(), element.getUpdated());
        } else {
            return new DesignAccumulator(element.getEvid(), element.getUuid(), element.getEsid(), element.getJson(), element.getStatus(), Checksum.of(element.getJson()), element.getLevels(), accumulator.getTiles(), element.getUpdated());
        }
    }
}
