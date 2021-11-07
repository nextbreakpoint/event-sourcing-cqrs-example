package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.TileRenderCompleted;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.DesignAccumulator;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (DESIGN_UUID, DESIGN_EVID, DESIGN_DATA, DESIGN_STATUS, DESIGN_CHECKSUM, DESIGN_UPDATED, DESIGN_TILES) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_UUID, MESSAGE_EVID, MESSAGE_TYPE, MESSAGE_BODY, MESSAGE_SOURCE, MESSAGE_PARTITIONKEY, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT * FROM MESSAGE WHERE MESSAGE_PARTITIONKEY = ?";

    private Tiles TILES_EMPTY = new Tiles(0, 0, Collections.emptySet(), Collections.emptySet());

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> insertDesign;
    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;
    private Single<Metadata> metadata;

    public CassandraStore(Supplier<CassandraClient> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    public Single<Void> appendMessage(UUID evid, Message message) {
        return withSession()
                .flatMap(session -> appendMessage(session, makeAppendMessageParams(evid, message)))
                .doOnError(err -> handleError(ERROR_INSERT_MESSAGE, err));
    }

    @Override
    public Single<Optional<Design>> updateDesign(UUID uuid) {
        return withSession()
                .flatMap(session -> updateDesign(session, uuid))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    private Single<CassandraClient> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            metadata = session.rxMetadata();
            insertDesign = session.rxPrepare(INSERT_DESIGN);
            insertMessage = session.rxPrepare(INSERT_MESSAGE);
            selectMessages = session.rxPrepare(SELECT_MESSAGES);
        }
        return Single.just(session);
    }

    private Single<Void> appendMessage(CassandraClient session, Object[] values) {
        return insertMessage
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Optional<Design>> updateDesign(CassandraClient session, UUID uuid) {
        return selectMessages
                .map(pst -> pst.bind(uuid.toString()))
                .flatMap(session::rxExecuteWithFullFetch)
                .observeOn(Schedulers.io())
                .map(this::mergeEvents)
                .observeOn(Schedulers.computation())
                .flatMap(result -> result.map(accumulator -> insertDesign(session, accumulator.toDesign())).orElseGet(() -> Single.just(Optional.empty())));
    }

    private Single<Optional<Design>> insertDesign(CassandraClient session, Design design) {
        return metadata
                .map(metadata -> metadata.getKeyspace("designs").orElseThrow())
                .map(keyspaceMetadata -> keyspaceMetadata.getUserDefinedType("LEVEL").orElseThrow())
                .flatMap(levelType -> insertDesign.map(pst -> pst.bind(makeDesignInsertParams(design, levelType))))
                .flatMap(session::rxExecute)
                .map(rs -> Optional.of(design));
    }

    private Optional<DesignAccumulator> mergeEvents(List<Row> rows) {
        return rows.stream().map(this::convertRow).reduce(this::mergeElement).filter(accumulator -> accumulator.getStatus() != null);
    }

    private DesignAccumulator convertRow(Row row) {
        final UUID evid = row.getUuid("MESSAGE_EVID");
        final String type = row.getString("MESSAGE_TYPE");
        final String body = row.getString("MESSAGE_BODY");
        switch (type) {
            case MessageType.DESIGN_INSERT_REQUESTED: {
                DesignInsertRequested event = Json.decodeValue(body, DesignInsertRequested.class);
                return new DesignAccumulator(event.getUuid(), evid, event.getJson(), "CREATED", Checksum.of(event.getJson()), new Date(event.getTimestamp()), createTilesMap());
            }
            case MessageType.DESIGN_UPDATE_REQUESTED: {
                DesignUpdateRequested event = Json.decodeValue(body, DesignUpdateRequested.class);
                return new DesignAccumulator(event.getUuid(), evid, event.getJson(), "UPDATED", Checksum.of(event.getJson()), new Date(event.getTimestamp()), null);
            }
            case MessageType.DESIGN_DELETE_REQUESTED: {
                DesignDeleteRequested event = Json.decodeValue(body, DesignDeleteRequested.class);
                return new DesignAccumulator(event.getUuid(), evid, null, "DELETED", null, new Date(event.getTimestamp()), null);
            }
            case MessageType.TILE_RENDER_COMPLETED: {
                TileRenderCompleted event = Json.decodeValue(body, TileRenderCompleted.class);
                return new DesignAccumulator(null, null, null, null, null, new Date(event.getEvid().timestamp()), createTilesMap(event));
            }
            default: {
                return new DesignAccumulator(null, null, null, null, null, null, null);
            }
        }
    }

    private Map<Integer, Tiles> createTilesMap() {
        return IntStream.range(0, 8)
                .mapToObj(level -> new Tiles(level, requestedTiles(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Map<Integer, Tiles> createTilesMap(TileRenderCompleted event) {
        return IntStream.of(event.getLevel())
                .mapToObj(level -> new Tiles(level, requestedTiles(level), getCompleted(event, level), getFailed(event, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<String> getCompleted(TileRenderCompleted event, int level) {
        return level == event.getLevel() && isCompleted(event) ? Set.of(event.getRow() + ":" + event.getCol()) : Collections.emptySet();
    }

    private Set<String> getFailed(TileRenderCompleted event, int level) {
        return level == event.getLevel() && isCompleted(event) ? Collections.emptySet() : Set.of(event.getRow() + ":" + event.getCol());
    }

    private Map<Integer, Tiles> createTilesMap(DesignAccumulator accumulator, DesignAccumulator element) {
        return IntStream.range(0, 8)
                .mapToObj(level -> new Tiles(level, requestedTiles(level), sumCompleted(accumulator, element, level), sumFailed(accumulator, element, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<String> sumCompleted(DesignAccumulator accumulator, DesignAccumulator element, int level) {
        HashSet<String> combined = new HashSet<>(accumulator.getTiles().get(level).getCompleted());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getCompleted());
        return combined;
    }

    private Set<String> sumFailed(DesignAccumulator accumulator, DesignAccumulator element, int level) {
        HashSet<String> combined = new HashSet<>(accumulator.getTiles().get(level).getFailed());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getFailed());
        return combined;
    }

    private boolean isCompleted(TileRenderCompleted event) {
        return event.getStatus().equalsIgnoreCase("COMPLETED");
    }

    private int requestedTiles(int level) {
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
            return new DesignAccumulator(accumulator.getUuid(), accumulator.getEvid(), accumulator.getJson(), accumulator.getStatus(), accumulator.getChecksum(), element.getUpdated(), createTilesMap(accumulator, element));
        }
        if ("DELETED".equals(element.getStatus())) {
            return new DesignAccumulator(element.getUuid(), accumulator.getEvid(), accumulator.getJson(), "DELETED", accumulator.getChecksum(), element.getUpdated(), accumulator.getTiles());
        } else {
            return new DesignAccumulator(element.getUuid(), accumulator.getEvid(), element.getJson(), "UPDATED", Checksum.of(element.getJson()), element.getUpdated(), accumulator.getTiles());
        }
    }

    private Object[] makeAppendMessageParams(UUID evid, Message message) {
        return new Object[] { message.getUuid(), evid, message.getType(), message.getBody(), message.getSource(), message.getPartitionKey(), Instant.ofEpochMilli(message.getTimestamp()) };
    }

    private Object[] makeDesignInsertParams(Design design, UserDefinedType levelType) {
        final Map<Integer, UdtValue> tiles = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, x -> createLevel(levelType, x)));
        return new Object[] { design.getUuid(), design.getEvid(), design.getJson(), design.getStatus(), Checksum.of(design.getJson()), design.getUpdated().toInstant(), tiles};
    }

    private UdtValue createLevel(UserDefinedType levelType, Tiles tiles) {
        return levelType.newValue().setInt(0, tiles.getRequested()).setInt(1, tiles.getCompleted().size()).setInt(2, tiles.getFailed().size());
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
