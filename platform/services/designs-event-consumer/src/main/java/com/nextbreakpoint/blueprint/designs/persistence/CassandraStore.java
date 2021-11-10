package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
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
    private static final String ERROR_FIND_DESIGN = "An error occurred while fetching a design";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (DESIGN_EVID, DESIGN_UUID, DESIGN_ESID, DESIGN_DATA, DESIGN_STATUS, DESIGN_CHECKSUM, DESIGN_LEVELS, DESIGN_UPDATED, DESIGN_TILES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_UUID, MESSAGE_OFFSET, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_KEY, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT * FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_OFFSET <= ? AND MESSAGE_OFFSET > ?";

    private Tiles TILES_EMPTY = new Tiles(0, 0, Collections.emptySet(), Collections.emptySet());

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> selectDesign;
    private Single<PreparedStatement> insertDesign;
    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;
    private Single<Metadata> metadata;

    public CassandraStore(Supplier<CassandraClient> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    public Single<Void> appendMessage(Message message) {
        return withSession()
                .flatMap(session -> appendMessage(session, makeAppendMessageParams(message)))
                .doOnError(err -> handleError(ERROR_INSERT_MESSAGE, err));
    }

    @Override
    public Single<Optional<Design>> updateDesign(UUID uuid, long esid) {
        return withSession()
                .flatMap(session -> findDesign(session, makeSelectDesignParams(uuid)).flatMap(design -> updateDesign(session, design.orElse(null), makeSelectMessagesParams(uuid, design.map(Design::getEsid).orElse(-1L), esid))))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<Optional<Design>> findDesign(UUID uuid) {
        return withSession()
                .flatMap(session -> findDesign(session, makeSelectDesignParams(uuid)))
                .doOnError(err -> handleError(ERROR_FIND_DESIGN, err));
    }

    private Single<CassandraClient> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            metadata = session.rxMetadata();
            selectDesign = session.rxPrepare(SELECT_DESIGN);
            insertDesign = session.rxPrepare(INSERT_DESIGN);
            insertMessage = session.rxPrepare(INSERT_MESSAGE);
            selectMessages = session.rxPrepare(SELECT_MESSAGES);
        }
        return Single.just(session);
    }

    private Single<Void> appendMessage(CassandraClient session, Object[] values) {
        return insertMessage
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Optional<Design>> updateDesign(CassandraClient session, Design design, Object[] values) {
        return selectMessages
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .observeOn(Schedulers.io())
                .map(rows -> mergeEvents(rows, design))
                .observeOn(Schedulers.computation())
                .flatMap(result -> result.map(accumulator -> insertDesign(session, accumulator.toDesign())).orElseGet(() -> Single.just(Optional.empty())));
    }

    private Single<Optional<Design>> findDesign(CassandraClient session, Object[] values) {
        return selectDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .observeOn(Schedulers.io())
                .map(this::convert);
    }

    private Single<Optional<Design>> insertDesign(CassandraClient session, Design design) {
        return metadata
                .map(metadata -> metadata.getKeyspace("designs").orElseThrow())
                .map(keyspaceMetadata -> keyspaceMetadata.getUserDefinedType("LEVEL").orElseThrow(() -> new RuntimeException("UDT not found: LEVEL")))
                .flatMap(levelType -> insertDesign.map(pst -> pst.bind(makeDesignInsertParams(design, levelType)).setConsistencyLevel(ConsistencyLevel.QUORUM)))
                .flatMap(session::rxExecute)
                .map(rs -> Optional.of(design));
    }

    private Optional<DesignAccumulator> mergeEvents(List<Row> rows, Design design) {
        if (design != null) {
            return Optional.of(rows.stream().map(this::convertRowToAccumulator).reduce(convertToAccumulator(design), this::mergeElement)).filter(accumulator -> accumulator.getStatus() != null);
        } else {
            return rows.stream().map(this::convertRowToAccumulator).reduce(this::mergeElement).filter(accumulator -> accumulator.getStatus() != null);
        }
    }

    private DesignAccumulator convertToAccumulator(Design design) {
        final Map<Integer, Tiles> tiles = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
        return new DesignAccumulator(design.getEvid(), design.getUuid(), design.getEsid(), design.getJson(), design.getStatus(), design.getChecksum(), design.getLevels(), tiles, design.getUpdated());
    }

    private Optional<Design> convert(List<Row> rows) {
        return rows.stream().findFirst().map(this::convertRowToDesign);
    }

    private DesignAccumulator convertRowToAccumulator(Row row) {
        final long offset = row.getLong("MESSAGE_OFFSET");
        final String type = row.getString("MESSAGE_TYPE");
        final String data = row.getString("MESSAGE_VALUE");
        final Instant timestamp = row.getInstant("MESSAGE_TIMESTAMP");
        switch (type) {
            case MessageType.DESIGN_INSERT_REQUESTED: {
                DesignInsertRequested event = Json.decodeValue(data, DesignInsertRequested.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, event.getData(), "CREATED", Checksum.of(event.getData()), event.getLevels(), createTilesMap(event.getLevels()), new Date(timestamp.toEpochMilli()));
            }
            case MessageType.DESIGN_UPDATE_REQUESTED: {
                DesignUpdateRequested event = Json.decodeValue(data, DesignUpdateRequested.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, event.getData(), "UPDATED", Checksum.of(event.getData()), event.getLevels(), null, new Date(timestamp.toEpochMilli()));
            }
            case MessageType.DESIGN_DELETE_REQUESTED: {
                DesignDeleteRequested event = Json.decodeValue(data, DesignDeleteRequested.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, null, "DELETED", null, 0, null, new Date(timestamp.toEpochMilli()));
            }
            case MessageType.TILE_RENDER_COMPLETED: {
                TileRenderCompleted event = Json.decodeValue(data, TileRenderCompleted.class);
                return new DesignAccumulator(event.getEvid(), event.getUuid(), offset, null, null, null, 0, createTilesMap(event), new Date(timestamp.toEpochMilli()));
            }
            default: {
                return new DesignAccumulator(null, null, 0, null, null, null, 0, null, null);
            }
        }
    }

    private Design convertRowToDesign(Row row) {
        final UUID evid = row.getUuid("DESIGN_EVID");
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final long esid = row.getLong("DESIGN_ESID");
        final String data = row.getString("DESIGN_DATA");
        final String status = row.getString("DESIGN_STATUS");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final int levels = row.getInt("DESIGN_LEVELS");
        final Instant updated = row.getInstant("DESIGN_UPDATED");
        final Map<Integer, UdtValue> tilesMap = row.getMap("DESIGN_TILES", Integer.class, UdtValue.class);
        final List<Tiles> tiles = tilesMap.entrySet().stream()
                .map(entry -> convertToTiles(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new Design(evid, uuid, esid, data, status, checksum, levels, tiles, toDate(updated));
    }

    private Tiles convertToTiles(Integer level, UdtValue udtValue) {
        return new Tiles(level, udtValue.getInt("REQUESTED"), udtValue.getSet("COMPLETED", Integer.class), udtValue.getSet("FAILED", Integer.class));
    }

    private Date toDate(Instant instant) {
        return new Date(instant != null ? instant.toEpochMilli() : 0L);
    }

    private Map<Integer, Tiles> createTilesMap(int levels) {
        return IntStream.range(0, levels)
                .mapToObj(level -> new Tiles(level, requestedTiles(level), Collections.emptySet(), Collections.emptySet()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Map<Integer, Tiles> createTilesMap(TileRenderCompleted event) {
        return IntStream.of(event.getLevel())
                .mapToObj(level -> new Tiles(level, requestedTiles(level), getCompleted(event, level), getFailed(event, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<Integer> getCompleted(TileRenderCompleted event, int level) {
        return level == event.getLevel() && isCompleted(event) ? Set.of((0xFFFF & event.getRow()) << 16 | (0xFFFF & event.getCol())) : Collections.emptySet();
    }

    private Set<Integer> getFailed(TileRenderCompleted event, int level) {
        return level == event.getLevel() && isCompleted(event) ? Collections.emptySet() : Set.of((0xFFFF & event.getRow()) << 16 | (0xFFFF & event.getCol()));
    }

    private Map<Integer, Tiles> createTilesMap(DesignAccumulator accumulator, DesignAccumulator element) {
        return IntStream.range(0, accumulator.getLevels())
                .mapToObj(level -> new Tiles(level, requestedTiles(level), sumCompleted(accumulator, element, level), sumFailed(accumulator, element, level)))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
    }

    private Set<Integer> sumCompleted(DesignAccumulator accumulator, DesignAccumulator element, int level) {
        Set<Integer> combined = new HashSet<>(accumulator.getTiles().get(level).getCompleted());
        combined.addAll(element.getTiles().getOrDefault(level, TILES_EMPTY).getCompleted());
        return combined;
    }

    private Set<Integer> sumFailed(DesignAccumulator accumulator, DesignAccumulator element, int level) {
        Set<Integer> combined = new HashSet<>(accumulator.getTiles().get(level).getFailed());
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
            return new DesignAccumulator(accumulator.getEvid(), accumulator.getUuid(), element.getEsid(), accumulator.getJson(), accumulator.getStatus(), accumulator.getChecksum(), accumulator.getLevels(), createTilesMap(accumulator, element), element.getUpdated());
        }
        if ("DELETED".equals(element.getStatus())) {
            return new DesignAccumulator(element.getEvid(), element.getUuid(), element.getEsid(), accumulator.getJson(), element.getStatus(), accumulator.getChecksum(), accumulator.getLevels(), accumulator.getTiles(), element.getUpdated());
        } else {
            return new DesignAccumulator(element.getEvid(), element.getUuid(), element.getEsid(), element.getJson(), element.getStatus(), Checksum.of(element.getJson()), element.getLevels(), accumulator.getTiles(), element.getUpdated());
        }
    }

    private Object[] makeAppendMessageParams(Message message) {
        return new Object[] { message.getPayload().getUuid(), message.getOffset(), message.getPayload().getType(), message.getPayload().getData(), message.getPayload().getSource(), message.getKey(), Instant.ofEpochMilli(message.getTimestamp()) };
    }

    private Object[] makeSelectMessagesParams(UUID uuid, long fromEesid, long toEsid) {
        return new Object[] { uuid.toString(), toEsid, fromEesid };
    }

    private Object[] makeSelectDesignParams(UUID uuid) {
        return new Object[] { uuid };
    }

    private Object[] makeDesignInsertParams(Design design, UserDefinedType levelType) {
        final Map<Integer, UdtValue> tiles = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, x -> createLevel(levelType, x)));
        return new Object[] { design.getEvid(), design.getUuid(), design.getEsid(), design.getJson(), design.getStatus(), Checksum.of(design.getJson()), design.getLevels(), design.getUpdated().toInstant(), tiles};
    }

    private UdtValue createLevel(UserDefinedType levelType, Tiles tiles) {
        return levelType.newValue()
                .setInt("REQUESTED", tiles.getRequested())
                .setSet("COMPLETED", tiles.getCompleted(), Integer.class)
                .setSet("FAILED", tiles.getFailed(), Integer.class);
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
