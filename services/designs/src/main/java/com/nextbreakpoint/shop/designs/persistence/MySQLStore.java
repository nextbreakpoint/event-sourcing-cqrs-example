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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nextbreakpoint.shop.common.vertx.TimeUtil.TIMESTAMP_EXT_PATTERN;
import static com.nextbreakpoint.shop.common.vertx.TimeUtil.TIMESTAMP_PATTERN;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MySQLStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(MySQLStore.class.getName());

    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, UPDATED=CURRENT_TIMESTAMP WHERE UUID=?";
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
                    final String created = row.getString("CREATED");
                    final String updated = row.getString("UPDATED");
                    final DesignDocument design = new DesignDocument(uuid, json, formatDate(parseDate(created)), formatDate(parseDate(updated)), parseDate(updated).getTime());
                    return new LoadDesignResponse(request.getUuid(), design);
                }).orElseGet(() -> new LoadDesignResponse(request.getUuid(), null)))
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
                .map(result -> {
                    final List<String> uuids = result
                            .stream()
                            .map(x -> x.getString("UUID"))
                            .collect(Collectors.toList());
                    return new ListDesignsResponse(uuids);
                })
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Optional<JsonObject> exactlyOne(List<JsonObject> list) {
        return Optional.ofNullable(list).filter(rows -> rows.size() == 1).map(rows -> rows.get(0));
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private JsonArray makeInsertParams(InsertDesignRequest request) {
        return new JsonArray().add(request.getUuid().toString()).add(request.getJson());
    }

    private JsonArray makeUpdateParams(UpdateDesignRequest request) {
        return new JsonArray().add(request.getJson()).add(request.getUuid().toString());
    }

    private JsonArray makeDeleteParams(DeleteDesignRequest request) {
        return new JsonArray().add(request.getUuid().toString());
    }

    private Date parseDate(String value) {
        try {
            final SimpleDateFormat df = new SimpleDateFormat(value.length() > 20 ? TIMESTAMP_EXT_PATTERN : TIMESTAMP_PATTERN);
            return df.parse(value);
        } catch (ParseException e) {
            logger.error("An error occurred while parsing date", e);
            return new Date(0);
        }
    }

    private String formatDate(Date value) {
        final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);
        return df.format(value);
    }
}
