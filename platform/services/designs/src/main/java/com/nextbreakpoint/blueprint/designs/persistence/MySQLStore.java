package com.nextbreakpoint.blueprint.designs.persistence;

import com.nextbreakpoint.blueprint.common.core.DesignDocument;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.TransactionIsolation;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class MySQLStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(MySQLStore.class.getName());

    private static final SQLOptions OPTIONS = new SQLOptions()
            .setTransactionIsolation(TransactionIsolation.READ_COMMITTED)
            .setQueryTimeout(10000);

    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGN_ENTITY (DESIGN_UUID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_CREATED, DESIGN_UPDATED) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    private static final String UPDATE_DESIGN = "UPDATE DESIGN_ENTITY SET DESIGN_DATA=?, DESIGN_CHECKSUM=?, DESIGN_UPDATED=CURRENT_TIMESTAMP WHERE DESIGN_UUID=?";
    private static final String SELECT_DESIGN = "SELECT * FROM DESIGN_ENTITY WHERE DESIGN_UUID = ?";
    private static final String SELECT_DESIGNS = "SELECT * FROM DESIGN_ENTITY";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGN_ENTITY WHERE DESIGN_UUID = ?";

    private final JDBCClient client;

    public MySQLStore(JDBCClient client) {
        this.client = client;
    }

    public Single<InsertDesignResponse> insertDesign(InsertDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doInsertDesign(conn, request))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    public Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doUpdateDesign(request, conn))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    public Single<LoadDesignResponse> loadDesign(LoadDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doLoadDesign(conn, request))
                .doOnError(err -> handleError(ERROR_LOAD_DESIGN, err));
    }

    public Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteDesign(conn, request))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    public Single<ListDesignsResponse> listDesigns(ListDesignsRequest request) {
        return withConnection()
                .flatMap(conn -> doListDesigns(conn, request))
                .doOnError(err -> handleError(ERROR_LIST_DESIGNS, err));
    }

    private Single<SQLConnection> withConnection() {
        return client.rxGetConnection()
                .doOnSuccess(conn -> conn.setOptions(OPTIONS))
                .doOnError(err -> handleError(ERROR_GET_CONNECTION, err));
    }

    private Single<InsertDesignResponse> doInsertDesign(SQLConnection conn, InsertDesignRequest request) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdateWithParams(INSERT_DESIGN, makeInsertParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new InsertDesignResponse(request.getUuid(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<UpdateDesignResponse> doUpdateDesign(UpdateDesignRequest request, SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdateWithParams(UPDATE_DESIGN, makeUpdateParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new UpdateDesignResponse(request.getUuid(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<LoadDesignResponse> doLoadDesign(SQLConnection conn, LoadDesignRequest request) {
        return conn.rxSetAutoCommit(true)
                .flatMap(x -> conn.rxQueryWithParams(SELECT_DESIGN, new JsonArray().add(request.getUuid().toString())))
                .map(ResultSet::getRows)
                .map(this::exactlyOne)
                .map(result -> result.map(this::toDocument).orElse(null))
                .map(document -> new LoadDesignResponse(UUID.fromString(document.getUuid()), document))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteDesignResponse> doDeleteDesign(SQLConnection conn, DeleteDesignRequest request) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdateWithParams(DELETE_DESIGN, makeDeleteParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteDesignResponse(request.getUuid(), result))
                .doOnError(err -> conn.rxRollback().subscribe())
                .doOnSuccess(result -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListDesignsResponse> doListDesigns(SQLConnection conn, ListDesignsRequest request) {
        return conn.rxSetAutoCommit(true)
                .flatMap(x -> conn.rxQuery(SELECT_DESIGNS))
                .map(ResultSet::getRows)
                .map(result -> result.stream().map(this::toDocumentNoJSON).collect(toList()))
                .map(ListDesignsResponse::new)
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private DesignDocument toDocument(JsonObject row) {
        final String uuid = row.getString("DESIGN_UUID");
        final String json = row.getString("DESIGN_DATA");
        final String updated = row.getString("DESIGN_UPDATED");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        return new DesignDocument(uuid, json, checksum, formatDate(convertStringToInstant(updated)));
    }

    private DesignDocument toDocumentNoJSON(JsonObject row) {
        final String uuid = row.getString("DESIGN_UUID");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        return new DesignDocument(uuid, null, checksum, null);
    }

    private Optional<JsonObject> exactlyOne(List<JsonObject> list) {
        return Optional.ofNullable(list).filter(rows -> rows.size() == 1).map(rows -> rows.get(0));
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private JsonArray makeInsertParams(InsertDesignRequest request) {
        return new JsonArray().add(request.getUuid().toString()).add(request.getJson()).add(computeChecksum(request.getJson()));
    }

    private JsonArray makeUpdateParams(UpdateDesignRequest request) {
        return new JsonArray().add(request.getJson()).add(computeChecksum(request.getJson())).add(request.getUuid().toString());
    }

    private JsonArray makeDeleteParams(DeleteDesignRequest request) {
        return new JsonArray().add(request.getUuid().toString());
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

    private Instant convertStringToInstant(String date) {
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(date));
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
