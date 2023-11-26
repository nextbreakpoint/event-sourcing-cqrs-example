package com.nextbreakpoint.blueprint.designs.aggregate;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.designs.common.Bitmap;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;
import org.apache.avro.specific.SpecificRecord;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Log4j2
public class DesignMergeStrategy {
    private static final String DESIGN_INSERT_REQUESTED_CLASS = DesignInsertRequested.getClassSchema().getFullName();
    private static final String DESIGN_UPDATE_REQUESTED_CLASS = DesignUpdateRequested.getClassSchema().getFullName();
    private static final String DESIGN_DELETE_REQUESTED_CLASS = DesignDeleteRequested.getClassSchema().getFullName();
    private static final String TILES_RENDERED_CLASS = TilesRendered.getClassSchema().getFullName();

    private static final Map<String, BiConsumer<Accumulator, InputMessage<SpecificRecord>>> handlers = Map.of(
            DESIGN_INSERT_REQUESTED_CLASS, new DesignInsertRequestedHandler(),
            DESIGN_UPDATE_REQUESTED_CLASS, new DesignUpdateRequestedHandler(),
            DESIGN_DELETE_REQUESTED_CLASS, new DesignDeleteRequestedHandler(),
            TILES_RENDERED_CLASS, new TilesRenderedHandler()
    );

    public static final BiConsumer<Accumulator, InputMessage<SpecificRecord>> DEFAULT_HANDLER = (accumulator, message) -> {};

    public Optional<Design> applyEvents(Design state, List<InputMessage<SpecificRecord>> messages) {
        log.trace("Apply events: {}", messages.size());

        return applyEvents(state != null ? () -> new Accumulator(state) : this::createState, messages);
    }

    private Optional<Design> applyEvents(Supplier<Accumulator> supplier, List<InputMessage<SpecificRecord>> messages) {
        return Optional.of(mergeEvents(messages, supplier)).filter(state -> state.design != null).map(state -> state.design);
    }

    private Accumulator mergeEvents(List<InputMessage<SpecificRecord>> messages, Supplier<Accumulator> supplier) {
        return messages.stream().collect(supplier, this::mergeEvent, (a, b) -> {
        });
    }

    private Accumulator createState() {
        return new Accumulator(null);
    }

    private static LocalDateTime toDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
    }

    private static class DesignInsertRequestedHandler implements BiConsumer<Accumulator, InputMessage<SpecificRecord>> {
        @Override
        public void accept(Accumulator state, InputMessage<SpecificRecord> message) {
            final SpecificRecord value = message.getValue().getData();
            final String token = message.getToken();
            final long timestamp = message.getTimestamp();

            if (state.design != null) {
                throw new IllegalStateException();
            }

            final DesignInsertRequested event = (DesignInsertRequested) value;

            final String checksum = Checksum.of(event.getData());

            final ByteBuffer bitmap = Bitmap.empty().toByteBuffer();

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
        }
    }

    private static class DesignUpdateRequestedHandler implements BiConsumer<Accumulator, InputMessage<SpecificRecord>> {
        @Override
        public void accept(Accumulator state, InputMessage<SpecificRecord> message) {
            final SpecificRecord value = message.getValue().getData();
            final String token = message.getToken();
            final long timestamp = message.getTimestamp();

            if (state.design == null) {
                throw new IllegalStateException();
            }

            if (state.design.getStatus().equals("DELETED")) {
                return;
            }

            final DesignUpdateRequested event = (DesignUpdateRequested) value;

            if (!state.design.getDesignId().equals(event.getDesignId())) {
                throw new IllegalStateException();
            }

            final String checksum = Checksum.of(event.getData());

            final int levels = !checksum.equals(state.design.getChecksum()) ? 3 : (!state.design.isPublished() && event.getPublished()) ? 8 : state.design.getLevels();

            final ByteBuffer bitmap = !checksum.equals(state.design.getChecksum()) ? Bitmap.empty().toByteBuffer() : state.design.getBitmap();

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
        }
    }

    private static class DesignDeleteRequestedHandler implements BiConsumer<Accumulator, InputMessage<SpecificRecord>> {
        @Override
        public void accept(Accumulator state, InputMessage<SpecificRecord> message) {
            final SpecificRecord value = message.getValue().getData();
            final String token = message.getToken();
            final long timestamp = message.getTimestamp();

            if (state.design == null) {
                throw new IllegalStateException();
            }

            if (state.design.getStatus().equals("DELETED")) {
                return;
            }

            final DesignDeleteRequested event = (DesignDeleteRequested) value;

            if (!state.design.getDesignId().equals(event.getDesignId())) {
                throw new IllegalStateException();
            }

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
        }
    }

    private static class TilesRenderedHandler implements BiConsumer<Accumulator, InputMessage<SpecificRecord>> {
        @Override
        public void accept(Accumulator state, InputMessage<SpecificRecord> message) {
            final SpecificRecord value = message.getValue().getData();
            final String token = message.getToken();
            final long timestamp = message.getTimestamp();

            if (state.design == null) {
                throw new IllegalStateException();
            }

            if (state.design.getStatus().equals("DELETED")) {
                return;
            }

            final TilesRendered event = (TilesRendered) value;

            if (!state.design.getDesignId().equals(event.getDesignId())) {
                throw new IllegalStateException();
            }

            if (state.design.getChecksum().equals(event.getChecksum())) {
                event.getTiles().forEach(tile -> Bitmap.of(state.design.getBitmap()).putTile(tile.getLevel(), tile.getRow(), tile.getCol()));
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
        }
    }

    private void mergeEvent(Accumulator state, InputMessage<SpecificRecord> message) {
        handlers.getOrDefault(message.getValue().getType(), DEFAULT_HANDLER).accept(state, message);
    }

    private static class Accumulator {
        private Design design;

        public Accumulator(Design design) {
            this.design = design;
        }
    }
}
