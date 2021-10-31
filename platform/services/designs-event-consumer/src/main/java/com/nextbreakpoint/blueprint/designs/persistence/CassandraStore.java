package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_UPDATE_AGGREGATE = "An error occurred while updating an aggregate";
    private static final String ERROR_INSERT_VERSION = "An error occurred while inserting a version";
    private static final String ERROR_INSERT_TILE = "An error occurred while inserting a tile";
    private static final String ERROR_UPDATE_TILE = "An error occurred while updating a tile";

    private static final String INSERT_DESIGN_EVENT = "INSERT INTO DESIGN_EVENT (DESIGN_UUID, DESIGN_DATA, DESIGN_STATUS, DESIGN_CHECKSUM, DESIGN_UPDATED, DESIGN_EVID) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_DESIGN_EVENTS = "SELECT * FROM DESIGN_EVENT WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN_AGGREGATE = "INSERT INTO DESIGN_AGGREGATE (DESIGN_UUID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_UPDATED) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_DESIGN_AGGREGATE = "UPDATE DESIGN_AGGREGATE SET DESIGN_DATA = ?, DESIGN_CHECKSUM = ?, DESIGN_UPDATED = ? WHERE DESIGN_UUID = ?";
    private static final String DELETE_DESIGN_AGGREGATE = "DELETE FROM DESIGN_AGGREGATE WHERE DESIGN_UUID = ?";
    private static final String INSERT_DESIGN_VERSION = "INSERT INTO DESIGN_VERSION (DESIGN_CHECKSUM, DESIGN_DATA, DESIGN_UPDATED) VALUES (?, ?, ?, toTimeStamp(now()))";
    private static final String INSERT_DESIGN_TILE = "INSERT INTO DESIGN_TILE (DESIGN_CHECKSUM, TILE_LEVEL, TILE_ROW, TILE_COL, TILE_STATUS) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
    private static final String UPDATE_DESIGN_TILE = "UPDATE DESIGN_TILE SET TILE_STATUS = ? WHERE DESIGN_CHECKSUM = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?";

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> insertDesignEvent;
    private Single<PreparedStatement> selectDesignEvents;
    private Single<PreparedStatement> insertDesignAggregate;
    private Single<PreparedStatement> updateDesignAggregate;
    private Single<PreparedStatement> deleteDesignAggregate;
    private Single<PreparedStatement> insertDesignVersion;
    private Single<PreparedStatement> insertDesignTile;
    private Single<PreparedStatement> updateDesignTile;

    public CassandraStore(Supplier<CassandraClient> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public Single<Void> insertDesign(UUID timeuuid, UUID designUuid, String json) {
        return withSession()
                .flatMap(session -> appendDesignEvent(session, makeDesignInsertParams(designUuid, timeuuid, json)))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<Void> updateDesign(UUID timeuuid, UUID designUuid, String json) {
        return withSession()
                .flatMap(session -> appendDesignEvent(session, makeDesignUpdateParams(designUuid, timeuuid, json)))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<Void> deleteDesign(UUID timeuuid, UUID designUuid) {
        return withSession()
                .flatMap(session -> appendDesignEvent(session, makeDesignDeleteParams(designUuid, timeuuid)))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<Optional<DesignChange>> updateDesignAggregate(UUID designUuid) {
        return withSession()
                .flatMap(session -> updateAggregate(session, designUuid))
                .doOnError(err -> handleError(ERROR_UPDATE_AGGREGATE, err));
    }

    @Override
    public Single<Void> insertDesignVersion(DesignVersion version) {
        return withSession()
                .flatMap(session -> insertVersion(session, makeInsertParams(version)))
                .doOnError(err -> handleError(ERROR_INSERT_VERSION, err));
    }

    @Override
    public Single<Void> insertDesignTile(DesignTile tile) {
        return withSession()
                .flatMap(session -> insertTile(session, makeInsertParams(tile)))
                .doOnError(err -> handleError(ERROR_INSERT_TILE, err));
    }

    @Override
    public Single<Void> updateDesignTile(DesignTile tile, String status) {
        return withSession()
                .flatMap(session -> updateTile(session, makeUpdateParams(tile, status)))
                .doOnError(err -> handleError(ERROR_UPDATE_TILE, err));
    }

    private Single<CassandraClient> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            insertDesignEvent = session.rxPrepare(INSERT_DESIGN_EVENT);
            selectDesignEvents = session.rxPrepare(SELECT_DESIGN_EVENTS);
            insertDesignAggregate = session.rxPrepare(INSERT_DESIGN_AGGREGATE);
            updateDesignAggregate = session.rxPrepare(UPDATE_DESIGN_AGGREGATE);
            deleteDesignAggregate = session.rxPrepare(DELETE_DESIGN_AGGREGATE);
            insertDesignVersion = session.rxPrepare(INSERT_DESIGN_VERSION);
            insertDesignTile = session.rxPrepare(INSERT_DESIGN_TILE);
            updateDesignTile = session.rxPrepare(UPDATE_DESIGN_TILE);
        }
        return Single.just(session);
    }

    private Single<Void> appendDesignEvent(CassandraClient session, Object[] values) {
        return insertDesignEvent
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Optional<DesignChange>> updateAggregate(CassandraClient session, UUID designUuid) {
        return selectDesignEvents
                .map(pst -> pst.bind(designUuid))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(this::mergeEvents)
                .flatMap(change -> doExecuteAggregate(session, change));
    }

    private Single<Void> insertVersion(CassandraClient session, Object[] values) {
        return insertDesignVersion
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Void> insertTile(CassandraClient session, Object[] values) {
        return insertDesignTile
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Void> updateTile(CassandraClient session, Object[] values) {
        return updateDesignTile
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> null);
    }

    private Single<Optional<DesignChange>> doExecuteAggregate(CassandraClient session, DesignChange change) {
        if (change == null) {
            return Single.just(Optional.ofNullable(null));
        }
        switch (change.getStatus().toLowerCase()) {
            case "created": {
                return insertDesignAggregate
                        .map(pst -> pst.bind(makeAggregateInsertParams(change)))
                        .flatMap(session::rxExecute)
                        .map(rs -> Optional.of(change));
            }
            case "updated": {
                return updateDesignAggregate
                        .map(pst -> pst.bind(makeAggregateUpdateParams(change)))
                        .flatMap(session::rxExecute)
                        .map(rs -> Optional.of(change));
            }
            case "deleted": {
                return deleteDesignAggregate
                        .map(pst -> pst.bind(makeAggregateDeleteParams(change)))
                        .flatMap(session::rxExecute)
                        .map(rs -> Optional.of(change));
            }
            default: {
                return Single.just(Optional.ofNullable(null));
            }
        }
    }

    private DesignChange mergeEvents(List<Row> rows) {
        return rows.stream()
                .map(this::toDesignChange)
                .reduce(this::mergeChanges)
                .orElse(null);
    }

    private DesignChange mergeChanges(DesignChange accumulator, DesignChange element) {
        if (element.getStatus().equalsIgnoreCase("deleted")) {
            return new DesignChange(accumulator.getUuid(), accumulator.getJson(), element.getStatus(), accumulator.getChecksum(), element.getModified());
        } else {
            return new DesignChange(accumulator.getUuid(), element.getJson(), element.getStatus(), element.getChecksum(), element.getModified());
        }
    }

    private Object[] makeDesignInsertParams(UUID uuid, UUID eventTimestamp, String json) {
        return new Object[] { uuid, json, "CREATED", Checksum.of(json), Instant.ofEpochMilli(Uuids.unixTimestamp(eventTimestamp)), eventTimestamp};
    }

    private Object[] makeDesignUpdateParams(UUID uuid, UUID eventTimestamp, String json) {
        return new Object[] { uuid, json, "UPDATED", Checksum.of(json), Instant.ofEpochMilli(Uuids.unixTimestamp(eventTimestamp)), eventTimestamp};
    }

    private Object[] makeDesignDeleteParams(UUID uuid, UUID eventTimestamp) {
        return new Object[] { uuid, null, "DELETED", null, Instant.ofEpochMilli(Uuids.unixTimestamp(eventTimestamp)), eventTimestamp};
    }

    private Object[] makeAggregateInsertParams(DesignChange change) {
        return new Object[] { change.getUuid(), change.getJson(), Checksum.of(change.getJson()), change.getModified().toInstant() };
    }

    private Object[] makeAggregateUpdateParams(DesignChange change) {
        return new Object[] { change.getJson(), Checksum.of(change.getJson()), change.getModified().toInstant(), change.getUuid() };
    }

    private Object[] makeAggregateDeleteParams(DesignChange change) {
        return new Object[] { change.getUuid() };
    }

    private Object[] makeInsertParams(DesignVersion version) {
        return new Object[] { version.getChecksum(), version.getData() };
    }

    private Object[] makeInsertParams(DesignTile tile) {
        return new Object[] { tile.getChecksum(), tile.getLevel(), tile.getY(), tile.getX() };
    }

    private Object[] makeUpdateParams(DesignTile tile, String status) {
        return new Object[] { status, tile.getChecksum(), tile.getLevel(), tile.getY(), tile.getX() };
    }

    private DesignChange toDesignChange(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String json = row.getString("DESIGN_DATA");
        final String status = row.getString("DESIGN_STATUS");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Instant updated = row.getInstant("DESIGN_UPDATED");
        final Date modified = updated != null ? new Date(updated.toEpochMilli()) : null;
        return new DesignChange(uuid, json, status, checksum, modified);
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
