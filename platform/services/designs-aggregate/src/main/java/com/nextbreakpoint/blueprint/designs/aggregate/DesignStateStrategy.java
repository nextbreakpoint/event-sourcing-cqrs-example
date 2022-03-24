package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Level;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.designs.model.Design;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DesignStateStrategy {
    private final Logger logger = LoggerFactory.getLogger(DesignStateStrategy.class.getName());

    public Optional<Design> applyEvents(Design state, List<InputMessage> messages) {
        logger.debug("Apply events: " + messages.size());
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

    private void mergeEvent(Accumulator state, InputMessage message) {
        final String type = message.getValue().getType();
        final String value = message.getValue().getData();
        final String token = message.getToken();
        final long timestamp = message.getTimestamp();
        switch (type) {
            case DesignInsertRequested.TYPE: {
                DesignInsertRequested event = Json.decodeValue(value, DesignInsertRequested.class);
                state.design = new Design(event.getDesignId(), event.getUserId(), event.getCommandId(), event.getData(), Checksum.of(event.getData()), token, "CREATED", event.getLevels(), createEmptyTiles(), toDateTime(timestamp));
                break;
            }
            case DesignUpdateRequested.TYPE: {
                DesignUpdateRequested event = Json.decodeValue(value, DesignUpdateRequested.class);
                state.design = new Design(event.getDesignId(), event.getUserId(), event.getCommandId(), event.getData(), Checksum.of(event.getData()), token, "UPDATED", event.getLevels(), createEmptyTiles(), toDateTime(timestamp));
                break;
            }
            case DesignDeleteRequested.TYPE: {
                DesignDeleteRequested event = Json.decodeValue(value, DesignDeleteRequested.class);
                state.design = new Design(event.getDesignId(), event.getUserId(), event.getCommandId(), state.design.getData(), state.design.getChecksum(), token, "DELETED", state.design.getLevels(), state.design.getTiles(), toDateTime(timestamp));
                break;
            }
            case TileRenderCompleted.TYPE: {
                TileRenderCompleted event = Json.decodeValue(value, TileRenderCompleted.class);
                state.design.getTiles().get(event.getLevel()).putTile(event.getRow(), event.getCol());
                state.design = new Design(event.getDesignId(), state.design.getUserId(), state.design.getCommandId(), state.design.getData(), state.design.getChecksum(), token, state.design.getStatus(), state.design.getLevels(), state.design.getTiles(), toDateTime(timestamp));
                System.out.println(state.design);
                break;
            }
            default: {
            }
        }
    }

    private List<Level> createEmptyTiles() {
        return IntStream.range(0, 8).mapToObj(Level::createEmpty).collect(Collectors.toList());
    }

    private class Accumulator {
        private Design design;

        public Accumulator(Design design) {
            this.design = design;
        }
    }
}
