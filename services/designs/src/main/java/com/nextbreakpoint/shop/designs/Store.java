package com.nextbreakpoint.shop.designs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Store {
    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, UPDATED=CURRENT_TIMESTAMP WHERE UUID=?";
    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGNS = "DELETE FROM DESIGNS";
    private static final String SELECT_DESIGNS = "SELECT DESIGNS.*,STATE.MODIFIED FROM DESIGNS LEFT JOIN STATE ON (STATE.NAME = 'designs')";
    private static final String SELECT_DESIGN_STATE = "SELECT UUID,UPDATED FROM DESIGNS WHERE UUID IN ($values)";
    private static final String SELECT_DESIGNS_STATE = "SELECT NAME,MODIFIED FROM STATE WHERE NAME = 'designs'";
    private static final String UPDATE_DESIGNS_STATE = "UPDATE STATE SET MODIFIED = CURRENT_TIMESTAMP WHERE NAME = 'designs'";

    private final JDBCClient client;

    public Store(JDBCClient client) {
        this.client = client;
    }

    public Single<Integer> insertDesign(UUID uuid, JsonObject json) {
        final JsonArray params = makeCreateParams(uuid, json.toString());

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doInsertDesign(params, conn)).doOnError(this::handleError);
    }

    public Single<Integer> updateDesign(UUID uuid, JsonObject json) {
        final JsonArray params = makeUpdateParams(uuid, json.toString());

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doUpdateDesign(params, conn)).doOnError(this::handleError);
    }

    public Single<Optional<JsonObject>> loadDesign(UUID uuid) {
        final JsonArray params = makeSelectParams(uuid);

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doLoadDesign(params, conn)).doOnError(this::handleError);
    }

    public Single<Integer> deleteDesign(UUID uuid) {
        final JsonArray params = makeSelectParams(uuid);

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doDeleteDesign(params, conn)).doOnError(this::handleError);
    }

    public Single<Integer> deleteDesigns() {
        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(this::doDeleteDesigns).doOnError(this::handleError);
    }

    public Single<List<JsonObject>> findDesigns() {
        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doFindDesigns(conn)).doOnError(this::handleError);
    }

    public Single<Optional<JsonObject>> getDesignsState() {
        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doGetDesignsState(conn)).doOnError(this::handleError);
    }

    public Single<List<JsonObject>> getDesignState(JsonArray uuids) {
        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> doGetDesignState(conn, uuids))
                .doOnError(this::handleError);
    }

    private Single<Integer> doInsertDesign(JsonArray params, SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdate(UPDATE_DESIGNS_STATE).timeout(10, SECONDS))
                .flatMap(x -> conn.rxUpdateWithParams(INSERT_DESIGN, params).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(x -> Single.just(x.getUpdated()));
    }

    private Single<Integer> doUpdateDesign(JsonArray params, SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdate(UPDATE_DESIGNS_STATE).timeout(10, SECONDS))
                .flatMap(x -> conn.rxUpdateWithParams(UPDATE_DESIGN, params).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(x -> Single.just(x.getUpdated()));
    }

    private Single<Optional<JsonObject>> doLoadDesign(JsonArray params, SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxQueryWithParams(SELECT_DESIGN, params).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(resultSet -> Single.just(optionalResult(resultSet)));
    }

    private Single<Integer> doDeleteDesign(JsonArray params, SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdate(UPDATE_DESIGNS_STATE).timeout(10, SECONDS))
                .flatMap(x -> conn.rxUpdateWithParams(DELETE_DESIGN, params).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(x -> Single.just(x.getUpdated()));
    }

    private Single<Integer> doDeleteDesigns(SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdate(UPDATE_DESIGNS_STATE).timeout(10, SECONDS))
                .flatMap(x -> conn.rxUpdate(DELETE_DESIGNS).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(x -> Single.just(x.getUpdated()));
    }

    private Single<List<JsonObject>> doFindDesigns(SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxQuery(SELECT_DESIGNS).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(resultSet -> Single.just(resultSet.getRows()));
    }

    private Single<Optional<JsonObject>> doGetDesignsState(SQLConnection conn) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxQuery(SELECT_DESIGNS_STATE).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(resultSet -> Single.just(optionalResult(resultSet)));
    }

    public Single<List<JsonObject>> doGetDesignState(SQLConnection conn, JsonArray uuids) {
        final String params = uuids.stream().map(x -> "?").collect(Collectors.joining(","));

        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxQueryWithParams(SELECT_DESIGN_STATE.replace("$values", params), uuids).timeout(10, SECONDS))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe())
                .flatMap(resultSet -> Single.just(resultSet.getRows()));
    }

    private Optional<JsonObject> optionalResult(ResultSet resultSet) {
        return Optional.ofNullable(resultSet.getRows()).filter(rows -> rows.size() == 1).map(rows -> rows.get(0));
    }

    private void handleError(Throwable err) {
        err.printStackTrace();
    }

    private JsonArray makeCreateParams(UUID uuid, String json) {
        final JsonArray params = new JsonArray();
        params.add(uuid.toString());
        params.add(json);
        return params;
    }

    private JsonArray makeUpdateParams(UUID uuid, String json) {
        final JsonArray params = new JsonArray();
        params.add(json);
        params.add(uuid.toString());
        return params;
    }

    private JsonArray makeSelectParams(UUID uuid) {
        JsonArray params = new JsonArray();
        params.add(uuid.toString());
        return params;
    }
}
