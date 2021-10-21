package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.DesignChange;
import com.nextbreakpoint.blueprint.designs.model.PersistenceResult;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_PUBLISH_DESIGN = "An error occurred while publishing a design";

    private static final String INSERT_DESIGN_EVENT = "INSERT INTO DESIGN_EVENT (DESIGN_UUID, DESIGN_DATA, DESIGN_STATUS, DESIGN_CHECKSUM, EVENT_UUID) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_DESIGN_EVENTS = "SELECT * FROM DESIGN_EVENT WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN = "INSERT INTO DESIGN (DESIGN_UUID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_UPDATED) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_DESIGN = "UPDATE DESIGN SET DESIGN_DATA = ?, DESIGN_CHECKSUM = ?, DESIGN_UPDATED = ? WHERE DESIGN_UUID = ?";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGN WHERE DESIGN_UUID = ?";
    private static final String UPDATE_DESIGN_EVENT = "UPDATE DESIGN_EVENT SET EVENT_PUBLISHED = ? WHERE DESIGN_UUID = ? AND EVENT_UUID = ?";

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> insertDesign;
    private Single<PreparedStatement> updateDesign;
    private Single<PreparedStatement> selectDesigns;
    private Single<PreparedStatement> insertDesignAggregate;
    private Single<PreparedStatement> updateDesignAggregate;
    private Single<PreparedStatement> deleteDesignAggregate;

    public CassandraStore(Supplier<CassandraClient> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    private Single<CassandraClient> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            insertDesign = session.rxPrepare(INSERT_DESIGN_EVENT);
            selectDesigns = session.rxPrepare(SELECT_DESIGN_EVENTS);
            insertDesignAggregate = session.rxPrepare(INSERT_DESIGN);
            updateDesignAggregate = session.rxPrepare(UPDATE_DESIGN);
            deleteDesignAggregate = session.rxPrepare(DELETE_DESIGN);
            updateDesign = session.rxPrepare(UPDATE_DESIGN_EVENT);
        }
        return Single.just(session);
    }

    @Override
    public Single<PersistenceResult<Void>> insertDesign(UUID uuid, UUID eventTimestamp, String json) {
        return withSession()
                .flatMap(session -> insertDesignUpdate(session, uuid, makeInsertParams(uuid, eventTimestamp, json)))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult<Void>> updateDesign(UUID uuid, UUID eventTimestamp, String json) {
        return withSession()
                .flatMap(session -> insertDesignUpdate(session, uuid, makeUpdateParams(uuid, eventTimestamp, json)))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult<Void>> deleteDesign(UUID uuid, UUID eventTimestamp) {
        return withSession()
                .flatMap(session -> insertDesignUpdate(session, uuid, makeDeleteParams(uuid, eventTimestamp)))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult<DesignChange>> updateDesignAggregate(UUID uuid, UUID eventTimestamp) {
        return withSession()
                .flatMap(session -> updateDesignAggregate(session, uuid, eventTimestamp))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult<Void>> publishDesign(UUID uuid, UUID eventTimestamp) {
        return withSession()
                .flatMap(session -> publishDesign(session, uuid, eventTimestamp))
                .doOnError(err -> handleError(ERROR_PUBLISH_DESIGN, err));
    }

    private Single<PersistenceResult<Void>> insertDesignUpdate(CassandraClient session, UUID uuid, Object[] values) {
        return insertDesign
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Single<PersistenceResult<DesignChange>> updateDesignAggregate(CassandraClient session, UUID uuid, UUID eventTimestamp) {
        return selectDesigns
                .map(pst -> pst.bind(uuid))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().map(this::toDesignChange).reduce(this::mergeChanges).orElse(null))
                .flatMap(change -> executeAggregate(session, uuid, eventTimestamp, change));
    }

    private Single<PersistenceResult<DesignChange>> executeAggregate(CassandraClient session, UUID uuid, UUID eventTimestamp, DesignChange change) {
        return doExecuteAggregate(session, uuid, change).map(result -> new PersistenceResult<>(uuid, change));
    }

    private Single<PersistenceResult<Void>> doExecuteAggregate(CassandraClient session, UUID uuid, DesignChange change) {
        switch (change.getStatus().toLowerCase()) {
            case "created": {
                return insertDesignAggregate
                        .map(pst -> pst.bind(makeInsertAggregateParams(change)))
                        .flatMap(session::rxExecute)
                        .map(rs -> new PersistenceResult<>(change.getUuid(), null));
            }
            case "updated": {
                return updateDesignAggregate
                        .map(pst -> pst.bind(makeUpdateAggregateParams(change)))
                        .flatMap(session::rxExecute)
                        .map(rs -> new PersistenceResult<>(change.getUuid(), null));
            }
            case "deleted": {
                return deleteDesignAggregate
                        .map(pst -> pst.bind(makeDeleteAggregateParams(change)))
                        .flatMap(session::rxExecute)
                        .map(rs -> new PersistenceResult<>(change.getUuid(), null));
            }
        }
        throw new IllegalStateException("Unknown status: " + change.getStatus());
    }

    private Single<PersistenceResult<Void>> publishDesign(CassandraClient session, UUID uuid, UUID eventTimestamp) {
        return updateDesign
                .map(pst -> pst.bind(Instant.now(), uuid, eventTimestamp))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private DesignChange mergeChanges(DesignChange designDocument1, DesignChange designDocument2) {
        if (designDocument2.getStatus().equalsIgnoreCase("deleted")) {
            return new DesignChange(designDocument1.getUuid(), designDocument1.getJson(), designDocument2.getStatus(), designDocument1.getChecksum(), designDocument2.getModified());
        } else {
            return new DesignChange(designDocument1.getUuid(), designDocument2.getJson(), designDocument2.getStatus(), designDocument2.getChecksum(), designDocument2.getModified());
        }
    }

    private DesignChange toDesignChange(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String json = row.getString("DESIGN_DATA");
        final String status = row.getString("DESIGN_STATUS");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Date modified = new Date(Uuids.unixTimestamp(Objects.requireNonNull(row.getUuid("EVENT_UUID"))));
        return new DesignChange(uuid, json, status, checksum, modified);
    }

    private Object[] makeInsertParams(UUID uuid, UUID eventTimestamp, String json) {
        return new Object[] { uuid, json, "CREATED", computeChecksum(json), eventTimestamp};
    }

    private Object[] makeUpdateParams(UUID uuid, UUID eventTimestamp, String json) {
        return new Object[] { uuid, json, "UPDATED", computeChecksum(json), eventTimestamp};
    }

    private Object[] makeDeleteParams(UUID uuid, UUID eventTimestamp) {
        return new Object[] { uuid, null, "DELETED", null, eventTimestamp};
    }

    private Object[] makeInsertAggregateParams(DesignChange change) {
        return new Object[] { change.getUuid(), change.getJson(), computeChecksum(change.getJson()), change.getModified().toInstant() };
    }

    private Object[] makeUpdateAggregateParams(DesignChange change) {
        return new Object[] { change.getJson(), computeChecksum(change.getJson()), change.getModified().toInstant(), change.getUuid() };
    }

    private Object[] makeDeleteAggregateParams(DesignChange change) {
        return new Object[] { change.getUuid() };
    }

    private String computeChecksum(String json) {
        try {
            final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            final MessageDigest md = MessageDigest.getInstance("MD5");
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute checksum", e);
        }
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
