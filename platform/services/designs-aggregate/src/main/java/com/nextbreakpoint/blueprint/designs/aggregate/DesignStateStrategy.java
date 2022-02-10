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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignStateStrategy {
    private Tiles TILES_EMPTY = new Tiles(0, 0, Collections.emptySet(), Collections.emptySet());

    public Optional<Design> mergeEvents(Design state, List<InputMessage> messages) {
        if (state != null) {
            return Optional.of(messages.stream().map(this::convertMessageToState).reduce(state, this::mergeState)).filter(a -> a.getStatus() != null);
        } else {
            return messages.stream().map(this::convertMessageToState).reduce(this::mergeState).filter(a -> a.getStatus() != null);
        }
    }

    private Design convertMessageToState(InputMessage message) {
        final long offset = message.getOffset();
        final String type = message.getValue().getType();
        final String value = message.getValue().getData();
        final long timestamp = message.getTimestamp();
        switch (type) {
            case DesignInsertRequested.TYPE: {
                DesignInsertRequested event = Json.decodeValue(value, DesignInsertRequested.class);
                return new Design(event.getEventId(), event.getDesignId(), offset, event.getData(), Checksum.of(event.getData()), "CREATED", event.getLevels(), createLevelsMap(event.getLevels()), toDateTime(timestamp));
            }
            case DesignUpdateRequested.TYPE: {
                DesignUpdateRequested event = Json.decodeValue(value, DesignUpdateRequested.class);
                return new Design(event.getEventId(), event.getDesignId(), offset, event.getData(), Checksum.of(event.getData()), "UPDATED", event.getLevels(), null, toDateTime(timestamp));
            }
            case DesignDeleteRequested.TYPE: {
                DesignDeleteRequested event = Json.decodeValue(value, DesignDeleteRequested.class);
                return new Design(event.getEventId(), event.getDesignId(), offset, null, null, "DELETED", 0, null, toDateTime(timestamp));
            }
            case TileRenderCompleted.TYPE: {
                TileRenderCompleted event = Json.decodeValue(value, TileRenderCompleted.class);
                return new Design(event.getEventId(), event.getDesignId(), offset, null, null, null, 0, createLevelsMap(event), toDateTime(timestamp));
            }
            default: {
                return new Design(null, null, 0, null, null, null, 0, null, null);
            }
        }
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

    private Map<Integer, Tiles> createLevelsMap(Design state, Design newState) {
        return IntStream.range(0, state.getLevels())
                .mapToObj(level -> new Tiles(level, getTilesCount(level), mergeCompleted(state, newState, level), mergeFailed(state, newState, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<Integer> mergeCompleted(Design state, Design element, int level) {
        Set<Integer> combined = new HashSet<>(state.getTiles().getOrDefault(level, TILES_EMPTY).getCompleted());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getCompleted());
        return combined;
    }

    private Set<Integer> mergeFailed(Design state, Design element, int level) {
        Set<Integer> combined = new HashSet<>(state.getTiles().getOrDefault(level, TILES_EMPTY).getFailed());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getFailed());
        return combined;
    }

    private boolean isCompleted(TileRenderCompleted event) {
        return event.getStatus().equalsIgnoreCase("COMPLETED");
    }

    private int getTilesCount(int level) {
        return (int) Math.rint(Math.pow(2, level * 2));
    }

    private Design mergeState(Design state, Design newState) {
        if (state.getStatus() == null) {
            return state;
        }
        if (newState.getStatus() == null && newState.getTiles() == null) {
            return state;
        }
        if (newState.getStatus() == null) {
            return new Design(state.getEventId(), state.getDesignId(), newState.getRevision(), state.getData(), state.getChecksum(), state.getStatus(), state.getLevels(), createLevelsMap(state, newState), newState.getModified());
        }
        if ("DELETED".equals(newState.getStatus())) {
            return new Design(newState.getEventId(), newState.getDesignId(), newState.getRevision(), state.getData(), state.getChecksum(), newState.getStatus(), state.getLevels(), state.getTiles(), newState.getModified());
        } else {
            return new Design(newState.getEventId(), newState.getDesignId(), newState.getRevision(), newState.getData(), Checksum.of(newState.getData()), newState.getStatus(), newState.getLevels(), state.getTiles(), newState.getModified());
        }
    }
}
