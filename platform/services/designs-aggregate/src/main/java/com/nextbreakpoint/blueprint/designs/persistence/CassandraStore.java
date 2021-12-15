package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.Tiles;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_SELECT_DESIGN = "An error occurred while fetching a design";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_SELECT_MESSAGES = "An error occurred while fetching messages";

    private static final String SELECT_DESIGN = "SELECT DESIGN_EVID, DESIGN_UUID, DESIGN_ESID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_LEVELS, DESIGN_TILES, DESIGN_UPDATED FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (DESIGN_EVID, DESIGN_UUID, DESIGN_ESID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_LEVELS, DESIGN_TILES, DESIGN_UPDATED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_UUID, MESSAGE_OFFSET, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_KEY, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT MESSAGE_UUID, MESSAGE_OFFSET, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_KEY, MESSAGE_TIMESTAMP FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_OFFSET <= ? AND MESSAGE_OFFSET > ?";

    private final String keyspace;
    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> selectDesign;
    private Single<PreparedStatement> insertDesign;
    private Single<PreparedStatement> deleteDesign;
    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;
    private Single<Metadata> metadata;

    public CassandraStore(String keyspace, Supplier<CassandraClient> supplier) {
        this.keyspace = Objects.requireNonNull(keyspace);
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public Single<List<InputMessage>> findMessages(UUID uuid, long fromEsid, long toEsid) {
        return withSession()
                .flatMap(session -> selectMessages(session, makeSelectMessagesParams(uuid, fromEsid, toEsid)))
                .doOnError(err -> handleError(ERROR_SELECT_MESSAGES, err));
    }

    @Override
    public Single<Void> appendMessage(InputMessage message) {
        return withSession()
                .flatMap(session -> insertMessage(session, makeInsertMessageParams(message)))
                .doOnError(err -> handleError(ERROR_INSERT_MESSAGE, err));
    }

    @Override
    public Single<Void> updateDesign(Design design) {
        return withSession()
                .flatMap(session -> getLevelType().flatMap(levelType -> insertDesign(session, makeInsertDesignParams(design, levelType))))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<Void> deleteDesign(Design design) {
        return withSession()
                .flatMap(session -> deleteDesign(session, makeDeleteDesignParams(design)))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<Optional<Design>> findDesign(UUID uuid) {
        return withSession()
                .flatMap(session -> selectDesign(session, makeSelectDesignParams(uuid)))
                .doOnError(err -> handleError(ERROR_SELECT_DESIGN, err));
    }

    private Single<UserDefinedType> getLevelType() {
        return metadata.map(metadata -> metadata.getKeyspace(keyspace).orElseThrow()).map(this::getLevelType);
    }

    private UserDefinedType getLevelType(KeyspaceMetadata metadata) {
        return metadata.getUserDefinedType("LEVEL").orElseThrow(() -> new RuntimeException("UDT not found: LEVEL"));
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
            deleteDesign = session.rxPrepare(DELETE_DESIGN);
            insertMessage = session.rxPrepare(INSERT_MESSAGE);
            selectMessages = session.rxPrepare(SELECT_MESSAGES);
        }
        return Single.just(session);
    }

    private Single<List<InputMessage>> selectMessages(CassandraClient session, Object[] values) {
        return selectMessages
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().map(this::convertRowToMessage).collect(Collectors.toList()));
    }

    private Single<Void> insertMessage(CassandraClient session, Object[] values) {
        return insertMessage
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Void> insertDesign(CassandraClient session, Object[] values) {
        return insertDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Void> deleteDesign(CassandraClient session, Object[] values) {
        return deleteDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Optional<Design>> selectDesign(CassandraClient session, Object[] values) {
        return selectDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(this::findFirstDesign);
    }

    public Optional<Design> findFirstDesign(List<Row> rows) {
        return rows.stream().findFirst().map(this::convertRowToDesign);
    }

    private Design convertRowToDesign(Row row) {
        final UUID evid = row.getUuid("DESIGN_EVID");
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final long esid = row.getLong("DESIGN_ESID");
        final String data = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final String status = row.getString("DESIGN_STATUS");
        final int levels = row.getInt("DESIGN_LEVELS");
        final Instant updated = row.getInstant("DESIGN_UPDATED");
        final Map<Integer, UdtValue> tilesMap = row.getMap("DESIGN_TILES", Integer.class, UdtValue.class);
        final Map<Integer, Tiles> tilesList = tilesMap.entrySet().stream()
                .map(entry -> convertUDTToTiles(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Tiles::getLevel, Function.identity()));
        return new Design(evid, uuid, esid, data, checksum, status, levels, tilesList, toDate(updated));
    }

    private InputMessage convertRowToMessage(Row row) {
        final String key = row.getString("MESSAGE_KEY");
        final UUID uuid = row.getUuid("MESSAGE_UUID");
        final String type = row.getString("MESSAGE_TYPE");
        final String value = row.getString("MESSAGE_VALUE");
        final long offset = row.getLong("MESSAGE_OFFSET");
        final String source = row.getString("MESSAGE_SOURCE");
        final Instant timestamp = row.getInstant("MESSAGE_TIMESTAMP");
        return new InputMessage(key, offset, new Payload(uuid, type, value, source), timestamp.toEpochMilli());
    }

    private Date toDate(Instant instant) {
        return new Date(instant != null ? instant.toEpochMilli() : 0L);
    }

    private Object[] makeSelectMessagesParams(UUID uuid, long fromEesid, long toEsid) {
        return new Object[] { uuid.toString(), toEsid, fromEesid };
    }

    private Object[] makeInsertMessageParams(InputMessage message) {
        return new Object[] { message.getValue().getUuid(), message.getOffset(), message.getValue().getType(), message.getValue().getData(), message.getValue().getSource(), message.getKey(), Instant.ofEpochMilli(message.getTimestamp()) };
    }

    private Object[] makeSelectDesignParams(UUID uuid) {
        return new Object[] { uuid };
    }

    private Object[] makeInsertDesignParams(Design design, UserDefinedType levelType) {
        final Map<Integer, UdtValue> levelsMap = design.getTiles().values().stream().collect(Collectors.toMap(Tiles::getLevel, x -> convertTilesToUDT(levelType, x)));
        return new Object[] { design.getEvid(), design.getUuid(), design.getEsid(), design.getJson(), Checksum.of(design.getJson()), design.getStatus(), design.getLevels(), levelsMap, design.getUpdated().toInstant() };
    }

    private Object[] makeDeleteDesignParams(Design design) {
        return new Object[] { design.getUuid() };
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
}
