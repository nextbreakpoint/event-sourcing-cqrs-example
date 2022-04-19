package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.Payload;
import com.nextbreakpoint.blueprint.designs.Store;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Log4j2
public class CassandraStore implements Store {
    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_SELECT_MESSAGES = "An error occurred while fetching messages";

    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_TOKEN <= ? AND MESSAGE_TOKEN > ?";

    private final Supplier<CqlSession> supplier;

    private final Tracer tracer;

    private CqlSession session;

    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;

    public CassandraStore(Supplier<CqlSession> supplier) {
        this.supplier = Objects.requireNonNull(supplier);

        tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
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
            insertMessage = Single.fromCallable(() -> session.prepare(INSERT_MESSAGE));
            selectMessages = Single.fromCallable(() -> session.prepare(SELECT_MESSAGES));
        }
        return Single.just(session);
    }

    private Single<List<InputMessage>> selectMessages(CqlSession session, Object[] values) {
        return selectMessages
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(stmt -> execute(session, stmt))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false).map(this::convertRowToMessage).collect(Collectors.toList()));
    }

    private Single<Void> insertMessage(CqlSession session, Object[] values) {
        return insertMessage
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(stmt -> execute(session, stmt))
                .map(rs -> null);
    }

    private ResultSet execute(CqlSession session, BoundStatement statement) {
        final Span executeSpan = tracer.spanBuilder("Execute statement").startSpan();

        try (Scope scope = executeSpan.makeCurrent()) {
            final Span span = Span.current();

            span.setAttribute("statement.query", statement.getPreparedStatement().getQuery());

            return session.execute(statement);
        } finally {
            executeSpan.end();
        }
    }

    private Single<Boolean> existsTable(CqlSession session, String tableName) {
        return Single.fromCallable(() -> session.execute("SELECT now() FROM " + tableName)).map(result -> true);
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

    private void handleError(String message, Throwable err) {
        log.error(message, err);
    }
}
