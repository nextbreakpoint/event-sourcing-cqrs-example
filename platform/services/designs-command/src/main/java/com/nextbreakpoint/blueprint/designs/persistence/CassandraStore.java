package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.designs.Store;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Single;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";

    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT MESSAGE_TOKEN, MESSAGE_KEY, MESSAGE_UUID, MESSAGE_TYPE, MESSAGE_VALUE, MESSAGE_SOURCE, MESSAGE_TIMESTAMP FROM MESSAGE WHERE MESSAGE_KEY = ? AND MESSAGE_TOKEN <= ? AND MESSAGE_TOKEN > ?";

    private final Supplier<CqlSession> supplier;

    private CqlSession session;

    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;

    public CassandraStore(Supplier<CqlSession> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
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

    private Single<Void> insertMessage(CqlSession session, Object[] values) {
        return insertMessage
                .map(pst -> pst.bind(values).setConsistencyLevel(ConsistencyLevel.QUORUM))
                .map(session::execute)
                .map(rs -> null);
    }

    private Single<Boolean> existsTable(CqlSession session, String tableName) {
        return Single.fromCallable(() -> session.execute("SELECT now() FROM " + tableName)).map(result -> true);
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
        logger.error(message, err);
    }
}
