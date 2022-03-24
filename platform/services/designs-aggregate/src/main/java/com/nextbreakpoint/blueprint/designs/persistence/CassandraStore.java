package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_SELECT_DESIGN = "An error occurred while fetching a design";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_SELECT_MESSAGES = "An error occurred while fetching messages";

    private static final String SELECT_DESIGN = "SELECT COMMAND_USER, COMMAND_UUID, DESIGN_UUID, DESIGN_REVISION, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_LEVELS, DESIGN_TILES, DESIGN_TIMESTAMP FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (COMMAND_USER, COMMAND_UUID, DESIGN_UUID, DESIGN_REVISION, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_LEVELS, DESIGN_TILES, DESIGN_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP, TRACING_TRACE_ID, TRACING_SPAN_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP, TRACING_TRACE_ID, TRACING_SPAN_ID FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_TOKEN <= ? AND MESSAGE_TOKEN > ?";

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
    public Single<List<InputMessage>> findMessages(UUID uuid, String fromRevision, String toRevision) {
        return withSession()
                .flatMap(session -> selectMessages(session, makeSelectMessagesParams(uuid, fromRevision, toRevision)))
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
                .flatMap(session -> getTileType().flatMap(levelType -> insertDesign(session, makeInsertDesignParams(design, levelType))))
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

    @Override
    public Single<Boolean> existsTable(String tableName) {
        return withSession()
                .flatMap(session -> existsTable(session, tableName));
    }

    private Optional<Design> findFirstDesign(List<Row> rows) {
        return rows.stream().findFirst().map(this::convertRowToDesign);
    }

    private Single<UserDefinedType> getTileType() {
        return metadata.map(metadata -> metadata.getKeyspace(keyspace).orElseThrow()).map(this::getTileType);
    }

    private UserDefinedType getTileType(KeyspaceMetadata metadata) {
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

    private Single<Boolean> existsTable(CassandraClient session, String tableName) {
        return session.rxExecute("SELECT now() FROM " + tableName).map(result -> true);
    }

    private Design convertRowToDesign(Row row) {
        final UUID userId = row.getUuid("COMMAND_USER");
        final UUID commandId = row.getUuid("COMMAND_UUID");
        final UUID designId = row.getUuid("DESIGN_UUID");
        final String data = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final String status = row.getString("DESIGN_STATUS");
        final int levels = row.getInt("DESIGN_LEVELS");
        final Instant updated = row.getInstant("DESIGN_TIMESTAMP");
        final String revision = row.getString("DESIGN_REVISION");
        final List<UdtValue> udfTiles = row.getList("DESIGN_TILES", UdtValue.class);
        final List<Level> tiles = IntStream.range(0, udfTiles.size())
                .mapToObj(level -> convertUDTToLevel(level, udfTiles.get(level)))
                .collect(Collectors.toList());
        return new Design(designId, userId, commandId, data, checksum, revision, status, levels, tiles, toDate(updated));
    }

    private InputMessage convertRowToMessage(Row row) {
        final String token  = row.getString("MESSAGE_TOKEN");
        final String key = row.getString("MESSAGE_KEY");
        final UUID uuid = row.getUuid("MESSAGE_UUID");
        final String type = row.getString("MESSAGE_TYPE");
        final String value = row.getString("MESSAGE_VALUE");
        final String source = row.getString("MESSAGE_SOURCE");
        final String traceId = row.getString("TRACING_TRACE_ID");
        final String spanId = row.getString("TRACING_SPAN_ID");
        final Instant timestamp = row.getInstant("MESSAGE_TIMESTAMP");
        final Payload payload = new Payload(uuid, type, value, source);
        final Tracing trace = new Tracing(traceId, spanId);
        final long messageTimestamp = timestamp != null ? timestamp.toEpochMilli() : 0L;
        return new InputMessage(key, token, payload, trace, messageTimestamp);
    }

    private LocalDateTime toDate(Instant instant) {
        return LocalDateTime.ofInstant(instant != null ? instant : Instant.ofEpochMilli(0), ZoneId.of("UTC"));
    }

    private Object[] makeSelectMessagesParams(UUID uuid, String fromRevision, String toRevision) {
        return new Object[] { uuid.toString(), toRevision, fromRevision };
    }

    private Object[] makeInsertMessageParams(InputMessage message) {
        return new Object[] {
                message.getToken(),
                message.getKey(),
                message.getValue().getUuid(),
                message.getValue().getType(),
                message.getValue().getData(),
                message.getValue().getSource(),
                Instant.ofEpochMilli(message.getTimestamp()),
                message.getTrace().getTraceId(),
                message.getTrace().getSpanId()
        };
    }

    private Object[] makeSelectDesignParams(UUID uuid) {
        return new Object[] { uuid };
    }

    private Object[] makeInsertDesignParams(Design design, UserDefinedType levelType) {
        final List<UdtValue> tiles = design.getTiles().stream()
                .map(level -> convertLevelToUDT(levelType, level))
                .collect(Collectors.toList());

        return new Object[] {
                design.getUserId(),
                design.getCommandId(),
                design.getDesignId(),
                design.getRevision(),
                design.getData(),
                Checksum.of(design.getData()),
                design.getStatus(),
                design.getLevels(),
                tiles,
                design.getLastModified().toInstant(ZoneOffset.UTC)
        };
    }

    private Object[] makeDeleteDesignParams(Design design) {
        return new Object[] { design.getDesignId() };
    }

    private Level convertUDTToLevel(Integer level, UdtValue udtValue) {
        return Level.of(level, udtValue.getList("TILES", Byte.class));
    }

    private UdtValue convertLevelToUDT(UserDefinedType definedType, Level level) {
        return definedType.newValue().setList("TILES", level.getTiles(), Byte.class);
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
