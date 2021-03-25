package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_SELECT_DESIGN = "An error occurred while fetching a design";
    private static final String ERROR_SELECT_VERSION = "An error occurred while fetching a version";
    private static final String ERROR_SELECT_RENDER = "An error occurred while fetching a render";
    private static final String ERROR_SELECT_TILE = "An error occurred while fetching a tile";
    private static final String ERROR_INSERT_VERSION = "An error occurred while inserting a version";
    private static final String ERROR_INSERT_RENDER = "An error occurred while inserting a render";
    private static final String ERROR_INSERT_TILE = "An error occurred while inserting a tile";
    private static final String ERROR_PUBLISH_VERSION = "An error occurred while publishing a version";
    private static final String ERROR_PUBLISH_RENDER = "An error occurred while publishing a render";
    private static final String ERROR_PUBLISH_TILE = "An error occurred while publishing a tile";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGN_ENTITY WHERE DESIGN_UUID = ?";
    private static final String SELECT_VERSION = "SELECT * FROM VERSION_ENTITY WHERE DESIGN_CHECKSUM = ?";
    private static final String SELECT_RENDER = "SELECT * FROM RENDER_ENTITY WHERE VERSION_UUID = ? AND RENDER_LEVEL = ?";
    private static final String SELECT_TILE = "SELECT * FROM TILE_ENTITY WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?";
    private static final String INSERT_VERSION = "INSERT INTO VERSION_ENTITY (VERSION_UUID, VERSION_CREATED, VERSION_UPDATED, DESIGN_DATA, DESIGN_CHECKSUM) VALUES (?, toTimeStamp(now()), toTimeStamp(now()), ?, ?)";
    private static final String INSERT_RENDER = "INSERT INTO RENDER_ENTITY (RENDER_UUID, RENDER_LEVEL, RENDER_CREATED, RENDER_UPDATED, VERSION_UUID) VALUES (?, ?, toTimeStamp(now()), toTimeStamp(now()), ?)";
    private static final String INSERT_TILE = "INSERT INTO TILE_ENTITY (TILE_UUID, TILE_LEVEL, TILE_ROW, TILE_COL, TILE_CREATED, TILE_UPDATED, VERSION_UUID) VALUES (?, ?, ?, ?, toTimeStamp(now()), toTimeStamp(now()), ?)";
    private static final String UPDATE_VERSION = "UPDATE VERSION_ENTITY SET VERSION_PUBLISHED = ? WHERE DESIGN_CHECKSUM = ?";
    private static final String UPDATE_RENDER = "UPDATE RENDER_ENTITY SET RENDER_PUBLISHED = ? WHERE VERSION_UUID = ? AND RENDER_LEVEL = ?";
    private static final String UPDATE_TILE = "UPDATE TILE_ENTITY SET TILE_PUBLISHED = ? WHERE VERSION_UUID = ? AND TILE_LEVEL = ? AND TILE_ROW = ? AND TILE_COL = ?";

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> selectDesign;
    private Single<PreparedStatement> selectVersion;
    private Single<PreparedStatement> selectRender;
    private Single<PreparedStatement> selectTile;
    private Single<PreparedStatement> insertVersion;
    private Single<PreparedStatement> insertRender;
    private Single<PreparedStatement> insertTile;
    private Single<PreparedStatement> updateVersion;
    private Single<PreparedStatement> updateRender;
    private Single<PreparedStatement> updateTile;

    public CassandraStore(Supplier<CassandraClient> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public Single<PersistenceResult<DesignDocument>> selectDesign(UUID designUuid) {
        return withSession()
                .flatMap(session -> selectDesign(session, designUuid))
                .doOnError(err -> handleError(ERROR_SELECT_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult<VersionDocument>> selectVersion(String checksum) {
        return withSession()
                .flatMap(session -> selectVersion(session, checksum))
                .doOnError(err -> handleError(ERROR_SELECT_VERSION, err));
    }

    @Override
    public Single<PersistenceResult<RenderDocument>> selectRender(UUID versionUuid, short level) {
        return withSession()
                .flatMap(session -> selectRender(session, versionUuid, level))
                .doOnError(err -> handleError(ERROR_SELECT_RENDER, err));
    }

    @Override
    public Single<PersistenceResult<TileDocument>> selectTile(UUID versionUuid, short level, short x, short y) {
        return withSession()
                .flatMap(session -> selectTile(session, versionUuid, level, x, y))
                .doOnError(err -> handleError(ERROR_SELECT_TILE, err));
    }

    @Override
    public Single<PersistenceResult<Void>> insertVersion(DesignVersion version) {
        return withSession()
                .flatMap(session -> insertVersion(session, version.getUuid(), makeInsertParams(version)))
                .doOnError(err -> handleError(ERROR_INSERT_VERSION, err));
    }

    @Override
    public Single<PersistenceResult<Void>> insertRender(DesignRender render) {
        return withSession()
                .flatMap(session -> insertRender(session, render.getUuid(), makeInsertParams(render)))
                .doOnError(err -> handleError(ERROR_INSERT_RENDER, err));
    }

    @Override
    public Single<PersistenceResult<Void>> insertTile(DesignTile tile) {
        return withSession()
                .flatMap(session -> insertTile(session, tile.getUuid(), makeInsertParams(tile)))
                .doOnError(err -> handleError(ERROR_INSERT_TILE, err));
    }

    @Override
    public Single<PersistenceResult<Void>> publishVersion(UUID uuid, String checksum) {
        return withSession()
                .flatMap(session -> publishVersion(session, uuid, checksum))
                .doOnError(err -> handleError(ERROR_PUBLISH_VERSION, err));
    }

    @Override
    public Single<PersistenceResult<Void>> publishRender(UUID uuid, UUID version, short level) {
        return withSession()
                .flatMap(session -> publishRender(session, uuid, version, level))
                .doOnError(err -> handleError(ERROR_PUBLISH_RENDER, err));
    }

    @Override
    public Single<PersistenceResult<Void>> publishTile(UUID uuid, UUID version, short level, short x, short y) {
        return withSession()
                .flatMap(session -> publishTile(session, uuid, version, level, x, y))
                .doOnError(err -> handleError(ERROR_PUBLISH_TILE, err));
    }

    private Single<CassandraClient> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            selectDesign = session.rxPrepare(SELECT_DESIGN);
            selectVersion = session.rxPrepare(SELECT_VERSION);
            selectRender = session.rxPrepare(SELECT_RENDER);
            selectTile = session.rxPrepare(SELECT_TILE);
            insertVersion = session.rxPrepare(INSERT_VERSION);
            insertRender = session.rxPrepare(INSERT_RENDER);
            insertTile = session.rxPrepare(INSERT_TILE);
            updateVersion = session.rxPrepare(UPDATE_VERSION);
            updateRender = session.rxPrepare(UPDATE_RENDER);
            updateTile = session.rxPrepare(UPDATE_TILE);
        }
        return Single.just(session);
    }

    private Single<PersistenceResult<DesignDocument>> selectDesign(CassandraClient session, UUID uuid) {
        return selectDesign
                .map(pst -> pst.bind(uuid))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().findFirst().map(this::toDesignDocument).map(document -> new PersistenceResult<>(UUID.fromString(document.getUuid()), document)).orElseGet(() -> new PersistenceResult<>(uuid, null)));
    }

    private Single<PersistenceResult<VersionDocument>> selectVersion(CassandraClient session, String checksum) {
        return selectVersion
                .map(pst -> pst.bind(checksum))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().findFirst().map(this::toVersionDocument).map(document -> new PersistenceResult<>(UUID.fromString(document.getUuid()), document)).orElseGet(() -> new PersistenceResult<>(null, null)));
    }

    private Single<PersistenceResult<RenderDocument>> selectRender(CassandraClient session, UUID uuid, short level) {
        return selectRender
                .map(pst -> pst.bind(uuid, level))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().findFirst().map(this::toRenderDocument).map(document -> new PersistenceResult<>(UUID.fromString(document.getUuid()), document)).orElseGet(() -> new PersistenceResult<>(null, null)));
    }

    private Single<PersistenceResult<TileDocument>> selectTile(CassandraClient session, UUID uuid, short level, short x, short y) {
        return selectTile
                .map(pst -> pst.bind(uuid, level, x, y))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().findFirst().map(this::toTileDocument).map(document -> new PersistenceResult<>(UUID.fromString(document.getUuid()), document)).orElseGet(() -> new PersistenceResult<>(null, null)));
    }

    private DesignDocument toDesignDocument(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String json = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Instant timestamp = row.getInstant("DESIGN_UPDATED");
        return new DesignDocument(Objects.requireNonNull(uuid).toString(), json, checksum, formatDate(timestamp));
    }

    private VersionDocument toVersionDocument(Row row) {
        final UUID uuid = row.getUuid("VERSION_UUID");
        final Instant created = row.getInstant("VERSION_CREATED");
        final Instant updated = row.getInstant("VERSION_UPDATED");
        final String json = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        return new VersionDocument(Objects.requireNonNull(uuid).toString(), json, checksum, formatDate(created), formatDate(updated));
    }

    private RenderDocument toRenderDocument(Row row) {
        final UUID uuid = row.getUuid("RENDER_UUID");
        final short level = row.getShort("RENDER_LEVEL");
        final Instant created = row.getInstant("RENDER_CREATED");
        final Instant updated = row.getInstant("RENDER_UPDATED");
        final UUID version = row.getUuid("VERSION_UUID");
        return new RenderDocument(Objects.requireNonNull(uuid).toString(), Objects.requireNonNull(version).toString(), level, formatDate(created), formatDate(updated));
    }

    private TileDocument toTileDocument(Row row) {
        final UUID uuid = row.getUuid("TILE_UUID");
        final short level = row.getShort("TILE_LEVEL");
        final short x = row.getShort("TILE_COL");
        final short y = row.getShort("TILE_ROW");
        final Instant created = row.getInstant("TILE_CREATED");
        final Instant updated = row.getInstant("TILE_UPDATED");
        final UUID version = row.getUuid("VERSION_UUID");
        return new TileDocument(Objects.requireNonNull(uuid).toString(), Objects.requireNonNull(version).toString(), level, x, y, formatDate(created), formatDate(updated));
    }

    private Single<PersistenceResult<Void>> insertVersion(CassandraClient session, UUID uuid, Object[] values) {
        return insertVersion
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Single<PersistenceResult<Void>> insertRender(CassandraClient session, UUID uuid, Object[] values) {
        return insertRender
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Single<PersistenceResult<Void>> insertTile(CassandraClient session, UUID uuid, Object[] values) {
        return insertTile
                .map(pst -> pst.bind(values))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Single<PersistenceResult<Void>> publishVersion(CassandraClient session, UUID uuid, String checksum) {
        return updateVersion
                .map(pst -> pst.bind(Instant.now(), checksum))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Single<PersistenceResult<Void>> publishRender(CassandraClient session, UUID uuid, UUID version, short level) {
        return updateRender
                .map(pst -> pst.bind(Instant.now(), version, level))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Single<PersistenceResult<Void>> publishTile(CassandraClient session, UUID uuid, UUID version, short level, short x, short y) {
        return updateTile
                .map(pst -> pst.bind(Instant.now(), version, level, x, y))
                .flatMap(session::rxExecute)
                .map(rs -> new PersistenceResult<>(uuid, null));
    }

    private Object[] makeInsertParams(DesignVersion version) {
        return new Object[] { version.getUuid(), version.getData(), version.getChecksum() };
    }

    private Object[] makeInsertParams(DesignRender render) {
        return new Object[] { render.getUuid(), render.getLevel(), render.getVersion().getUuid() };
    }

    private Object[] makeInsertParams(DesignTile tile) {
        return new Object[] { tile.getUuid(), tile.getLevel(), tile.getY(), tile.getX(), tile.getVersion().getUuid() };
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
