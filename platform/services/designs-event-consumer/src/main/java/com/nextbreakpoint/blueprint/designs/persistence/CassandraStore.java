package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.DesignAggregate;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.DesignAccumulator;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_FIND_DESIGN = "An error occurred while fetching a design";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (DESIGN_EVID, DESIGN_UUID, DESIGN_ESID, DESIGN_DATA, DESIGN_STATUS, DESIGN_CHECKSUM, DESIGN_LEVELS, DESIGN_UPDATED, DESIGN_TILES) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_UUID, MESSAGE_OFFSET, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_KEY, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT * FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_OFFSET <= ? AND MESSAGE_OFFSET > ?";

    private final DesignAggregate designAggregate = new DesignAggregate();

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

    public Single<Void> appendMessage(InputMessage message) {
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
                .map(rows -> designAggregate.mergeEvents(design != null ? convertToAccumulator(design) : null, rows))
                .observeOn(Schedulers.computation())
                .flatMap(result -> result.map(accumulator -> insertDesign(session, accumulator.toDesign())).orElseGet(() -> Single.just(Optional.empty())));
    }

    private Single<Optional<Design>> findDesign(CassandraClient session, Object[] values) {
        return selectDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(this::findFirstDesign);
    }

    private Single<Optional<Design>> insertDesign(CassandraClient session, Design design) {
        return metadata
                .map(metadata -> metadata.getKeyspace("designs").orElseThrow())
                .map(keyspaceMetadata -> keyspaceMetadata.getUserDefinedType("LEVEL").orElseThrow(() -> new RuntimeException("UDT not found: LEVEL")))
                .flatMap(levelType -> insertDesign.map(pst -> pst.bind(makeDesignInsertParams(design, levelType)).setConsistencyLevel(ConsistencyLevel.QUORUM)))
                .flatMap(session::rxExecute)
                .map(rs -> Optional.of(design));
    }

    public Optional<Design> findFirstDesign(List<Row> rows) {
        return rows.stream().findFirst().map(this::convertRowToDesign);
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
        final List<Tiles> tilesList = tilesMap.entrySet().stream()
                .map(entry -> convertUDTToTiles(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new Design(evid, uuid, esid, data, status, checksum, levels, tilesList, toDate(updated));
    }

    private Date toDate(Instant instant) {
        return new Date(instant != null ? instant.toEpochMilli() : 0L);
    }

    private Object[] makeAppendMessageParams(InputMessage message) {
        return new Object[] { message.getValue().getUuid(), message.getOffset(), message.getValue().getType(), message.getValue().getData(), message.getValue().getSource(), message.getKey(), Instant.ofEpochMilli(message.getTimestamp()) };
    }

    private Object[] makeSelectMessagesParams(UUID uuid, long fromEesid, long toEsid) {
        return new Object[] { uuid.toString(), toEsid, fromEesid };
    }

    private Object[] makeSelectDesignParams(UUID uuid) {
        return new Object[] { uuid };
    }

    private Object[] makeDesignInsertParams(Design design, UserDefinedType levelType) {
        final Map<Integer, UdtValue> levelsMap = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, x -> convertTilesToUDT(levelType, x)));
        return new Object[] { design.getEvid(), design.getUuid(), design.getEsid(), design.getJson(), design.getStatus(), Checksum.of(design.getJson()), design.getLevels(), design.getUpdated().toInstant(), levelsMap};
    }

    private Tiles convertUDTToTiles(Integer level, UdtValue udtValue) {
        final int requested = udtValue.getInt("REQUESTED");
        final Set<Integer> completed = udtValue.getSet("COMPLETED", Integer.class);
        final Set<Integer> failed = udtValue.getSet("FAILED", Integer.class);
        return new Tiles(level, requested, completed, failed);
    }

    private UdtValue convertTilesToUDT(UserDefinedType levelType, Tiles tiles) {
        return levelType.newValue()
                .setInt("REQUESTED", tiles.getRequested())
                .setSet("COMPLETED", tiles.getCompleted(), Integer.class)
                .setSet("FAILED", tiles.getFailed(), Integer.class);
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private DesignAccumulator convertToAccumulator(Design design) {
        final Map<Integer, Tiles> tilesMap = design.getTiles().stream().collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
        return new DesignAccumulator(design.getEvid(), design.getUuid(), design.getEsid(), design.getJson(), design.getStatus(), design.getChecksum(), design.getLevels(), tilesMap, design.getUpdated());
    }
}
