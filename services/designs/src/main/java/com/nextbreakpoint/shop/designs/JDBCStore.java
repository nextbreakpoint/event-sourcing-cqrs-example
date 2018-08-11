package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.get.GetStatusRequest;
import com.nextbreakpoint.shop.designs.get.GetStatusResponse;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.list.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.list.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.list.ListStatusRequest;
import com.nextbreakpoint.shop.designs.list.ListStatusResponse;
import com.nextbreakpoint.shop.designs.load.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.load.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.model.Design;
import com.nextbreakpoint.shop.designs.model.Status;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_EXT_PATTERN;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;
import static java.util.concurrent.TimeUnit.SECONDS;

public class JDBCStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(JDBCStore.class.getName());

    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_DELETE_DESIGNS = "An error occurred while deleting all designs";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";
    private static final String ERROR_GET_DESIGNS_STATUS = "An error occurred while loading designs status";
    private static final String ERROR_GET_DESIGN_STATUS = "An error occurred while loading design status";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, UPDATED=CURRENT_TIMESTAMP WHERE UUID=?";
    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGNS = "DELETE FROM DESIGNS";
    private static final String SELECT_DESIGNS = "SELECT DESIGNS.*,STATE.MODIFIED FROM DESIGNS LEFT JOIN STATE ON (STATE.NAME = 'designs')";
    private static final String SELECT_DESIGNS_BY_STATUS = "SELECT UUID,UPDATED FROM DESIGNS WHERE UUID IN ($values)";
    private static final String SELECT_STATUS = "SELECT NAME,MODIFIED FROM STATE WHERE NAME = 'designs'";
    private static final String UPDATE_STATUS = "UPDATE STATE SET MODIFIED = CURRENT_TIMESTAMP WHERE NAME = 'designs'";

    private static final int EXECUTE_TIMEOUT = 10;
    private static final int CONNECT_TIMEOUT = 5;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    private final JDBCClient client;

    public JDBCStore(JDBCClient client) {
        this.client = client;
    }

    @Override
    public Single<InsertDesignResponse> insertDesign(InsertDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doInsertDesign(conn, request))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doUpdateDesign(request, conn))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<LoadDesignResponse> loadDesign(LoadDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doLoadDesign(conn, request))
                .doOnError(err -> handleError(ERROR_LOAD_DESIGN, err));
    }

    @Override
    public Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteDesign(conn, request))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<DeleteDesignsResponse> deleteDesigns(DeleteDesignsRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteDesigns(conn))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGNS, err));
    }

    @Override
    public Single<ListDesignsResponse> listDesigns(ListDesignsRequest request) {
        return withConnection()
                .flatMap(conn -> doListDesigns(conn))
                .doOnError(err -> handleError(ERROR_LIST_DESIGNS, err));
    }

    @Override
    public Single<GetStatusResponse> getStatus(GetStatusRequest request) {
        return withConnection()
                .flatMap(conn -> doGetStatus(conn, request))
                .doOnError(err -> handleError(ERROR_GET_DESIGN_STATUS, err));
    }

    @Override
    public Single<ListStatusResponse> listStatus(ListStatusRequest request) {
        return withConnection()
                .flatMap(conn -> doListStatus(conn, request))
                .doOnError(err -> handleError(ERROR_GET_DESIGNS_STATUS, err));
    }

    private Single<SQLConnection> withConnection() {
        return client.rxGetConnection()
                .timeout(CONNECT_TIMEOUT, SECONDS)
                .doOnError(err -> handleError(ERROR_GET_CONNECTION, err));
    }

    private Single<InsertDesignResponse> doInsertDesign(SQLConnection conn, InsertDesignRequest request) {
        return conn.rxSetAutoCommit(false)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .flatMap(x -> conn.rxUpdate(UPDATE_STATUS).timeout(EXECUTE_TIMEOUT, SECONDS))
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
                .flatMap(x -> conn.rxUpdate(UPDATE_STATUS).timeout(EXECUTE_TIMEOUT, SECONDS))
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
                    final Design design = new Design(uuid, json, formatDate(parseDate(created)), formatDate(parseDate(updated)), parseDate(updated).getTime());
                    return new LoadDesignResponse(request.getUuid(), design);
                }).orElseGet(() -> new LoadDesignResponse(request.getUuid(), null)))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteDesignResponse> doDeleteDesign(SQLConnection conn, DeleteDesignRequest request) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdate(UPDATE_STATUS).timeout(EXECUTE_TIMEOUT, SECONDS))
                .flatMap(x -> conn.rxUpdateWithParams(DELETE_DESIGN, makeDeleteParams(request)).timeout(EXECUTE_TIMEOUT, SECONDS))
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteDesignResponse(request.getUuid(), result))
                .doOnError(err -> conn.rxRollback().subscribe())
                .doOnSuccess(result -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteDesignsResponse> doDeleteDesigns(SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .flatMap(x -> conn.rxUpdate(UPDATE_STATUS).timeout(EXECUTE_TIMEOUT, SECONDS))
                .flatMap(x -> conn.rxUpdate(DELETE_DESIGNS).timeout(EXECUTE_TIMEOUT, SECONDS))
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteDesignsResponse(result))
                .doOnError(err -> conn.rxRollback().subscribe())
                .doOnSuccess(result -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListDesignsResponse> doListDesigns(SQLConnection conn) {
        return conn.rxQuery(SELECT_DESIGNS)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(result -> {
                    final Long updated = result
                            .stream()
                            .findFirst()
                            .map(json -> json.getString("MODIFIED"))
                            .map(modified -> parseDate(modified).getTime())
                            .orElse(0L);
                    final List<String> uuids = result
                            .stream()
                            .map(x -> x.getString("UUID"))
                            .collect(Collectors.toList());
                    return new ListDesignsResponse(updated, uuids);
                })
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<GetStatusResponse> doGetStatus(SQLConnection conn, GetStatusRequest request) {
        return conn.rxQuery(SELECT_STATUS)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(this::exactlyOne)
                .map(result -> {
                    final Long updated = result
                            .map(row -> row.getString("MODIFIED"))
                            .map(modified -> parseDate(modified).getTime())
                            .orElse(0L);
                    return new GetStatusResponse(new Status("designs", updated));
                })
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListStatusResponse> doListStatus(SQLConnection conn, ListStatusRequest request) {
        return conn.rxQueryWithParams(SELECT_DESIGNS_BY_STATUS.replace("$values", makeValues(request.getUuids())), makeListStatusParams(request))
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(result -> {
                    final List<Status> list = result.stream()
                            .map(row -> {
                                final String uuid = row.getString("UUID");
                                final String updated = row.getString("UPDATED");
                                return new Status(uuid, parseDate(updated).getTime());
                            })
                            .collect(Collectors.toList());
                    return new ListStatusResponse(list);
                })
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private String makeValues(List<String> uuids) {
        return uuids.stream().map(x1 -> "?").collect(Collectors.joining(","));
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

    private JsonArray makeListStatusParams(ListStatusRequest request) {
        return request.getUuids().stream().collect(() -> new JsonArray(), (a, x) -> a.add(x), (a, b) -> a.addAll(b));
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
