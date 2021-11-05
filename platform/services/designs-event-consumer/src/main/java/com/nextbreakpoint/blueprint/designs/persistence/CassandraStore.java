package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.events.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.DesignAccumulator;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_MESSAGE = "An error occurred while inserting a message";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (DESIGN_UUID, DESIGN_EVID, DESIGN_DATA, DESIGN_STATUS, DESIGN_CHECKSUM, DESIGN_UPDATED) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MESSAGE = "INSERT INTO MESSAGE (MESSAGE_UUID, MESSAGE_EVID, MESSAGE_TYPE, MESSAGE_BODY, MESSAGE_SOURCE, MESSAGE_PARTITIONKEY, MESSAGE_TIMESTAMP) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES = "SELECT * FROM MESSAGE WHERE MESSAGE_PARTITIONKEY = ?";

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> insertDesign;
    private Single<PreparedStatement> insertMessage;
    private Single<PreparedStatement> selectMessages;

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
                .map(this::mergeEvents)
                .flatMap(result -> result.map(accumulator -> insertDesign(session, accumulator.toDesign())).orElseGet(() -> Single.just(Optional.empty())));
    }

    private Single<Optional<Design>> insertDesign(CassandraClient session, Design design) {
        return insertDesign
                .map(pst -> pst.bind(makeDesignInsertParams(design)))
                .flatMap(session::rxExecute)
                .map(rs -> Optional.of(design));
    }

    private Optional<DesignAccumulator> mergeEvents(List<Row> rows) {
        return rows.stream().map(this::convertRow).reduce(this::mergeElement);
    }

    private DesignAccumulator convertRow(Row row) {
        final UUID evid = row.getUuid("MESSAGE_EVID");
        final String type = row.getString("MESSAGE_TYPE");
        final String body = row.getString("MESSAGE_BODY");
        switch (type) {
            case MessageType.DESIGN_INSERT_REQUESTED: {
                DesignInsertRequested event = Json.decodeValue(body, DesignInsertRequested.class);
                return new DesignAccumulator(event.getUuid(), evid, event.getJson(), "CREATED", Checksum.of(event.getJson()), new Date(event.getTimestamp()));
            }
            case MessageType.DESIGN_UPDATE_REQUESTED: {
                DesignUpdateRequested event = Json.decodeValue(body, DesignUpdateRequested.class);
                return new DesignAccumulator(event.getUuid(), evid, event.getJson(), "UPDATED", Checksum.of(event.getJson()), new Date(event.getTimestamp()));
            }
            case MessageType.DESIGN_DELETE_REQUESTED: {
                DesignDeleteRequested event = Json.decodeValue(body, DesignDeleteRequested.class);
                return new DesignAccumulator(event.getUuid(), evid, null, "DELETED", null, new Date(event.getTimestamp()));
            }
            default: {
                return new DesignAccumulator(null, null, null, null, null, null);
            }
        }
    }

    private DesignAccumulator mergeElement(DesignAccumulator accumulator, DesignAccumulator element) {
        if (element.getStatus() == null) {
            return accumulator;
        }
        if ("DELETED".equals(element.getStatus())) {
            return new DesignAccumulator(element.getUuid(), accumulator.getEvid(), accumulator.getJson(), "DELETED", accumulator.getChecksum(), element.getUpdated());
        } else {
            return new DesignAccumulator(element.getUuid(), accumulator.getEvid(), element.getJson(), "UPDATED", Checksum.of(element.getJson()), element.getUpdated());
        }
    }

//    Instant.ofEpochMilli(Uuids.unixTimestamp(evid))

    private Object[] makeAppendMessageParams(UUID evid, Message message) {
        return new Object[] { message.getUuid(), evid, message.getType(), message.getBody(), message.getSource(), message.getPartitionKey(), Instant.ofEpochMilli(message.getTimestamp()) };
    }

    private Object[] makeDesignInsertParams(Design design) {
        return new Object[] { design.getUuid(), design.getEvid(), design.getJson(), design.getStatus(), Checksum.of(design.getJson()), design.getUpdated().toInstant() };
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
