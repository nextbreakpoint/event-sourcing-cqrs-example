package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.common.core.Tracing;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Log4j2
public class CassandraStore implements Store {
    private static final String ERROR_SELECT_DESIGN = "An error occurred while fetching a design";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_SELECT_MESSAGES = "An error occurred while fetching messages";

    private static final String SELECT_DESIGN = "SELECT COMMAND_USER, COMMAND_UUID, DESIGN_UUID, DESIGN_REVISION, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_PUBLISHED, DESIGN_LEVELS, DESIGN_BITMAP, DESIGN_CREATED, DESIGN_UPDATED FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (COMMAND_USER, COMMAND_UUID, DESIGN_UUID, DESIGN_REVISION, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_STATUS, DESIGN_PUBLISHED, DESIGN_LEVELS, DESIGN_BITMAP, DESIGN_CREATED, DESIGN_UPDATED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_TOKEN <= ? AND MESSAGE_TOKEN > ?";

    private final Supplier<CqlSession> supplier;

    private CqlSession session;

    private Single<PreparedStatement> selectDesign;
    private Single<PreparedStatement> insertDesign;
    private Single<PreparedStatement> deleteDesign;
    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;

    public CassandraStore(Supplier<CqlSession> supplier) {
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
                .flatMap(session -> insertDesign(session, makeInsertDesignParams(design)))
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

    private Single<CqlSession> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            selectDesign = Single.fromCallable(() -> session.prepare(SELECT_DESIGN));
            insertDesign = Single.fromCallable(() -> session.prepare(INSERT_DESIGN));
            deleteDesign = Single.fromCallable(() -> session.prepare(DELETE_DESIGN));
            insertMessage = Single.fromCallable(() -> session.prepare(INSERT_MESSAGE));
            selectMessages = Single.fromCallable(() -> session.prepare(SELECT_MESSAGES));
        }
        return Single.just(session);
    }

    private Single<List<InputMessage>> selectMessages(CqlSession session, Object[] values) {
        return selectMessages
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(session::execute)
                .map(rows -> StreamSupport.stream(rows.spliterator(), false).map(this::convertRowToMessage).collect(Collectors.toList()));
    }

    private Single<Void> insertMessage(CqlSession session, Object[] values) {
        return insertMessage
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(session::execute)
                .map(rs -> null);
    }

    private Single<Void> insertDesign(CqlSession session, Object[] values) {
        return insertDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(session::execute)
                .map(rs -> null);
    }

    private Single<Void> deleteDesign(CqlSession session, Object[] values) {
        return deleteDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(session::execute)
                .map(rs -> null);
    }

    private Single<Optional<Design>> selectDesign(CqlSession session, Object[] values) {
        return selectDesign
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(session::execute)
                .map(rows -> StreamSupport.stream(rows.spliterator(), false).limit(1).findFirst())
                .map(result -> result.map(this::convertRowToDesign));
    }

    private Single<Boolean> existsTable(CqlSession session, String tableName) {
        return Single.fromCallable(() -> session.execute("SELECT now() FROM " + tableName)).map(result -> true);
    }

    private Design convertRowToDesign(Row row) {
        final UUID userId = row.getUuid("COMMAND_USER");
        final UUID commandId = row.getUuid("COMMAND_UUID");
        final UUID designId = row.getUuid("DESIGN_UUID");
        final String data = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final String status = row.getString("DESIGN_STATUS");
        final boolean published = row.getBoolean("DESIGN_PUBLISHED");
        final int levels = row.getInt("DESIGN_LEVELS");
        final Instant created = row.getInstant("DESIGN_CREATED");
        final Instant updated = row.getInstant("DESIGN_UPDATED");
        final String revision = row.getString("DESIGN_REVISION");
        final ByteBuffer bitmap = row.getByteBuffer("DESIGN_BITMAP");
        return new Design(designId, userId, commandId, data, checksum, revision, status, published, levels, bitmap, toDate(created), toDate(updated));
    }

    private InputMessage convertRowToMessage(Row row) {
        final String token  = row.getString("MESSAGE_TOKEN");
        final String key = row.getString("MESSAGE_KEY");
        final UUID uuid = row.getUuid("MESSAGE_UUID");
        final String type = row.getString("MESSAGE_TYPE");
        final String value = row.getString("MESSAGE_VALUE");
        final String source = row.getString("MESSAGE_SOURCE");
        final Instant timestamp = row.getInstant("MESSAGE_TIMESTAMP");
        final Payload payload = new Payload(uuid, type, value, source);
        final long messageTimestamp = timestamp != null ? timestamp.toEpochMilli() : 0L;
        return new InputMessage(key, token, payload, messageTimestamp);
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
                Instant.ofEpochMilli(message.getTimestamp())
        };
    }

    private Object[] makeSelectDesignParams(UUID uuid) {
        return new Object[] { uuid };
    }

    private Object[] makeInsertDesignParams(Design design) {
        return new Object[] {
                design.getUserId(),
                design.getCommandId(),
                design.getDesignId(),
                design.getRevision(),
                design.getData(),
                Checksum.of(design.getData()),
                design.getStatus(),
                design.isPublished(),
                design.getLevels(),
                design.getBitmap(),
                design.getCreated().toInstant(ZoneOffset.UTC),
                design.getUpdated().toInstant(ZoneOffset.UTC)
        };
    }

    private Object[] makeDeleteDesignParams(Design design) {
        return new Object[] { design.getDesignId() };
    }

    private void handleError(String message, Throwable err) {
        log.error(message, err);
    }
}
