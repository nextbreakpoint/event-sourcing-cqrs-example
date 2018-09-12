package com.nextbreakpoint.shop.designs.persistence;

import com.nextbreakpoint.shop.common.model.DesignDocument;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MySQLStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(MySQLStore.class.getName());

    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED, CHECKSUM) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, CHECKSUM=?, UPDATED=CURRENT_TIMESTAMP WHERE UUID=?";
    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS WHERE UUID = ?";
    private static final String SELECT_DESIGNS = "SELECT * FROM DESIGNS";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGNS WHERE UUID = ?";

    private static final int EXECUTE_TIMEOUT = 10;
    private static final int CONNECT_TIMEOUT = 5;

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
                .flatMap(conn -> doListDesigns(conn))
                .doOnError(err -> handleError(ERROR_LIST_DESIGNS, err));
    }

    private Single<SQLConnection> withConnection() {
        return client.rxGetConnection()
                .timeout(CONNECT_TIMEOUT, SECONDS)
                .doOnError(err -> handleError(ERROR_GET_CONNECTION, err));
    }

    private Single<InsertDesignResponse> doInsertDesign(SQLConnection conn, InsertDesignRequest request) {
        return conn.rxSetAutoCommit(false)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .flatMap(x -> conn.rxUpdateWithParams(INSERT_DESIGN, makeInsertParams(request)).timeout(EXECUTE_TIMEOUT, SECONDS))
                .map(UpdateResult::getUpdated)
                .map(result -> new InsertDesignResponse(request.getUuid(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<UpdateDesignResponse> doUpdateDesign(UpdateDesignRequest request, SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .flatMap(x -> conn.rxUpdateWithParams(UPDATE_DESIGN, makeUpdateParams(request)).timeout(EXECUTE_TIMEOUT, SECONDS))
                .map(UpdateResult::getUpdated)
                .map(result -> new UpdateDesignResponse(request.getUuid(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<LoadDesignResponse> doLoadDesign(SQLConnection conn, LoadDesignRequest request) {
        return conn.rxQueryWithParams(SELECT_DESIGN, new JsonArray().add(request.getUuid().toString()))
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(this::exactlyOne)
                .map(result -> result.map(row -> {
                    final String uuid = row.getString("UUID");
                    final String json = row.getString("JSON");
                    final String updated = row.getString("UPDATED");
                    final String checksum = row.getString("CHECKSUM");
                    return new DesignDocument(uuid, json, "UPDATED", checksum, formatDate(convertStringToInstant(updated)));
                })
                .map(document -> new LoadDesignResponse(request.getUuid(), document))
                .orElseGet(() -> new LoadDesignResponse(request.getUuid(), null)))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteDesignResponse> doDeleteDesign(SQLConnection conn, DeleteDesignRequest request) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdateWithParams(DELETE_DESIGN, makeDeleteParams(request)).timeout(EXECUTE_TIMEOUT, SECONDS))
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteDesignResponse(request.getUuid(), result))
                .doOnError(err -> conn.rxRollback().subscribe())
                .doOnSuccess(result -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListDesignsResponse> doListDesigns(SQLConnection conn) {
        return conn.rxQuery(SELECT_DESIGNS)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(result -> result.stream()
                        .map(row -> {
                            final String uuid = row.getString("UUID");
                            final String checksum = row.getString("CHECKSUM");
                            return new DesignDocument(uuid, null, null, checksum, null);
                        })
                        .collect(Collectors.toList()))
                .map(documents -> new ListDesignsResponse(documents))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
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
            final byte[] bytes = json.getBytes("UTF-8");
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
