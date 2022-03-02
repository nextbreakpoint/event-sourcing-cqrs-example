package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.Tiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignStateStrategy {
    private Tiles TILES_EMPTY = new Tiles(0, 0, Collections.emptySet(), Collections.emptySet());

    public Optional<Design> applyEvents(Design state, List<InputMessage> messages) {
        return applyEvents(state != null ? () -> new Accumulator(state) : this::createState, messages);
    }

    private Optional<Design> applyEvents(Supplier<Accumulator> supplier, List<InputMessage> messages) {
        return Optional.of(mergeEvents(messages, supplier)).filter(state -> state.design != null).map(state -> state.design);
    }

    private Accumulator mergeEvents(List<InputMessage> messages, Supplier<Accumulator> supplier) {
        return messages.stream().collect(supplier, this::mergeEvent, (a, b) -> {});
    }

    private Accumulator createState() {
        return new Accumulator(null);
    }

    private LocalDateTime toDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
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

    private Map<Integer, Tiles> createLevelsMap(Design state, Map<Integer, Tiles> elementTiles) {
        return IntStream.range(0, state.getLevels())
                .mapToObj(level -> new Tiles(level, getTilesCount(level), mergeCompleted(level, state.getTiles(), elementTiles), mergeFailed(level, state.getTiles(), elementTiles)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<Integer> mergeCompleted(int level, Map<Integer, Tiles> tiles, Map<Integer, Tiles> elementTiles) {
        Set<Integer> combined = new HashSet<>(tiles.getOrDefault(level, TILES_EMPTY).getCompleted());
        combined.addAll(elementTiles.getOrDefault(level, TILES_EMPTY).getCompleted());
        return combined;
    }

    private Set<Integer> mergeFailed(int level, Map<Integer, Tiles> tiles, Map<Integer, Tiles> elementTiles) {
        Set<Integer> combined = new HashSet<>(tiles.getOrDefault(level, TILES_EMPTY).getFailed());
        combined.addAll(elementTiles.getOrDefault(level, TILES_EMPTY).getFailed());
        return combined;
    }

    private boolean isCompleted(TileRenderCompleted event) {
        return event.getStatus().equalsIgnoreCase("COMPLETED");
    }

    private int getTilesCount(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    private void mergeEvent(Accumulator state, InputMessage message) {
        final String type = message.getValue().getType();
        final String value = message.getValue().getData();
        final String token = message.getToken();
        final long timestamp = message.getTimestamp();
        switch (type) {
            case DesignInsertRequested.TYPE: {
                DesignInsertRequested event = Json.decodeValue(value, DesignInsertRequested.class);
                state.design = new Design(event.getDesignId(), event.getUserId(), event.getCommandId(), event.getData(), Checksum.of(event.getData()), token, "CREATED", event.getLevels(), createLevelsMap(event.getLevels()), toDateTime(timestamp));
                break;
            }
            case DesignUpdateRequested.TYPE: {
                DesignUpdateRequested event = Json.decodeValue(value, DesignUpdateRequested.class);
                state.design = new Design(event.getDesignId(), event.getUserId(), event.getCommandId(), event.getData(), Checksum.of(event.getData()), token, "UPDATED", event.getLevels(), createLevelsMap(event.getLevels()), toDateTime(timestamp));
                break;
            }
            case DesignDeleteRequested.TYPE: {
                DesignDeleteRequested event = Json.decodeValue(value, DesignDeleteRequested.class);
                state.design = new Design(event.getDesignId(), event.getUserId(), event.getCommandId(), state.design.getData(), state.design.getChecksum(), token, "DELETED", state.design.getLevels(), state.design.getTiles(), toDateTime(timestamp));
                break;
            }
            case TileRenderCompleted.TYPE: {
                TileRenderCompleted event = Json.decodeValue(value, TileRenderCompleted.class);
                state.design = new Design(event.getDesignId(), state.design.getUserId(), state.design.getCommandId(), state.design.getData(), state.design.getChecksum(), token, state.design.getStatus(), state.design.getLevels(), createLevelsMap(state.design, createLevelsMap(event)), toDateTime(timestamp));
                break;
            }
            default: {
            }
        }
    }

    private class Accumulator {
        private Design design;

        public Accumulator(Design design) {
            this.design = design;
        }
    }
}
