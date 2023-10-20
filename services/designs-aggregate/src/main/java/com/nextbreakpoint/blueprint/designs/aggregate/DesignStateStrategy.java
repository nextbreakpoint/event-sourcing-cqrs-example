package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.TilesBitmap;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TilesRendered;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Log4j2
public class DesignStateStrategy {
    public Optional<Design> applyEvents(Design state, List<InputMessage> messages) {
        log.trace("Apply events: {}", messages.size());

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
                final DesignInsertRequested event = Json.decodeValue(value, DesignInsertRequested.class);

                final String checksum = Checksum.of(event.getData());

                final ByteBuffer bitmap = TilesBitmap.empty().getBitmap();

                state.design = Design.builder()
                        .withDesignId(event.getDesignId())
                        .withUserId(event.getUserId())
                        .withCommandId(event.getCommandId())
                        .withData(event.getData())
                        .withChecksum(checksum)
                        .withRevision(token)
                        .withStatus("CREATED")
                        .withPublished(false)
                        .withLevels(3)
                        .withBitmap(bitmap)
                        .withCreated(toDateTime(timestamp))
                        .withUpdated(toDateTime(timestamp))
                        .build();

                break;
            }
            case DesignUpdateRequested.TYPE: {
                final DesignUpdateRequested event = Json.decodeValue(value, DesignUpdateRequested.class);

                final String checksum = Checksum.of(event.getData());

                final int levels = !checksum.equals(state.design.getChecksum()) ? 3 : (!state.design.isPublished() && event.getPublished()) ? 8 : state.design.getLevels();

                final ByteBuffer bitmap = !checksum.equals(state.design.getChecksum()) ? TilesBitmap.empty().getBitmap() : state.design.getBitmap();

                state.design = Design.builder()
                        .withDesignId(event.getDesignId())
                        .withUserId(event.getUserId())
                        .withCommandId(event.getCommandId())
                        .withData(event.getData())
                        .withChecksum(checksum)
                        .withRevision(token)
                        .withStatus("UPDATED")
                        .withPublished(event.getPublished())
                        .withLevels(levels)
                        .withBitmap(bitmap)
                        .withCreated(state.design.getCreated())
                        .withUpdated(toDateTime(timestamp))
                        .build();

                break;
            }
            case DesignDeleteRequested.TYPE: {
                final DesignDeleteRequested event = Json.decodeValue(value, DesignDeleteRequested.class);

                state.design = Design.builder()
                        .withDesignId(event.getDesignId())
                        .withUserId(event.getUserId())
                        .withCommandId(event.getCommandId())
                        .withData(state.design.getData())
                        .withChecksum(state.design.getChecksum())
                        .withRevision(token)
                        .withStatus("DELETED")
                        .withPublished(state.design.isPublished())
                        .withLevels(state.design.getLevels())
                        .withBitmap(state.design.getBitmap())
                        .withCreated(state.design.getCreated())
                        .withUpdated(toDateTime(timestamp))
                        .build();

                break;
            }
            case TilesRendered.TYPE: {
                final TilesRendered event = Json.decodeValue(value, TilesRendered.class);

                if (state.design.getChecksum().equals(event.getChecksum())) {
                    event.getTiles().forEach(tile -> TilesBitmap.of(state.design.getBitmap()).putTile(tile.getLevel(), tile.getRow(), tile.getCol()));
                } else {
                    log.trace("Skipping event: {}", event);
                }

                state.design = Design.builder()
                        .withDesignId(event.getDesignId())
                        .withUserId(state.design.getUserId())
                        .withCommandId(state.design.getCommandId())
                        .withData(state.design.getData())
                        .withChecksum(state.design.getChecksum())
                        .withRevision(token)
                        .withStatus(state.design.getStatus())
                        .withPublished(state.design.isPublished())
                        .withLevels(state.design.getLevels())
                        .withBitmap(state.design.getBitmap())
                        .withCreated(state.design.getCreated())
                        .withUpdated(toDateTime(timestamp))
                        .build();

                break;
            }
            default: {
            }
        }
    }

    private static class Accumulator {
        private Design design;

        public Accumulator(Design design) {
            this.design = design;
        }
    }
}
