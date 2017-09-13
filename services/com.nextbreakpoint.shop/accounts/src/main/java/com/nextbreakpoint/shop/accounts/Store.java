package com.nextbreakpoint.shop.accounts;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Single;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Store {
    private static final String INSERT_ACCOUNT = "INSERT INTO ACCOUNTS (UUID, NAME, EMAIL, ROLE) VALUES (?, ?, ?, ?)";
    private static final String SELECT_ACCOUNT = "SELECT * FROM ACCOUNTS WHERE UUID = ?";
    private static final String DELETE_ACCOUNT = "DELETE FROM ACCOUNTS WHERE UUID = ?";
    private static final String DELETE_ACCOUNTS = "DELETE FROM ACCOUNTS";
    private static final String SELECT_ACCOUNTS = "SELECT * FROM ACCOUNTS";
    private static final String SELECT_ACCOUNTS_BY_EMAIL = "SELECT * FROM ACCOUNTS WHERE EMAIL = ?";

    private final JDBCClient client;

    public Store(JDBCClient client) {
        this.client = client;
    }

    public Single<Integer> insertAccount(UUID uuid, String name, String email, String role) {
        final JsonArray params = makeCreateParams(uuid, name, email, role);

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> conn.rxUpdateWithParams(INSERT_ACCOUNT, params).timeout(10, SECONDS).doAfterTerminate(() -> conn.close()))
                .flatMap(x -> Single.just(x.getUpdated()))
                .doOnError(err -> err.printStackTrace());
    }

    public Single<Optional<JsonObject>> loadAccount(UUID uuid) {
        final JsonArray params = makeSelectParams(uuid);

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> conn.rxQueryWithParams(SELECT_ACCOUNT, params).timeout(10, SECONDS).doAfterTerminate(() -> conn.close()))
                .flatMap(resultSet -> Single.just(Optional.ofNullable(resultSet.getRows()).filter(rows -> rows.size() == 1).map(rows -> rows.get(0))))
                .doOnError(err -> err.printStackTrace());
    }

    public Single<Integer> deleteAccount(UUID uuid) {
        final JsonArray params = makeSelectParams(uuid);

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> conn.rxUpdateWithParams(DELETE_ACCOUNT, params).timeout(10, SECONDS).doAfterTerminate(() -> conn.close()))
                .flatMap(x -> Single.just(x.getUpdated()))
                .doOnError(err -> err.printStackTrace());
    }

    public Single<Integer> deleteAccounts() {
        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> conn.rxUpdate(DELETE_ACCOUNTS).timeout(10, SECONDS).doAfterTerminate(() -> conn.close()))
                .flatMap(x -> Single.just(x.getUpdated()))
                .doOnError(err -> err.printStackTrace());
    }

    public Single<List<JsonObject>> findAccounts() {
        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> conn.rxQuery(SELECT_ACCOUNTS).timeout(10, SECONDS).doAfterTerminate(() -> conn.close()))
                .flatMap(resultSet -> Single.just(resultSet.getRows()))
                .doOnError(err -> err.printStackTrace());
    }

    public Single<List<JsonObject>> findAccounts(String email) {
        final JsonArray params = makeSelectParams(email);

        return client.rxGetConnection().timeout(5, SECONDS)
                .flatMap(conn -> conn.rxQueryWithParams(SELECT_ACCOUNTS_BY_EMAIL, params).timeout(10, SECONDS).doAfterTerminate(() -> conn.close()))
                .flatMap(resultSet -> Single.just(resultSet.getRows()))
                .doOnError(err -> err.printStackTrace());
    }

    private JsonArray makeCreateParams(UUID uuid, String name, String email, String role) {
        final JsonArray params = new JsonArray();
        params.add(uuid.toString());
        params.add(name);
        params.add(email);
        params.add(role);
        return params;
    }

    private JsonArray makeSelectParams(UUID uuid) {
        JsonArray params = new JsonArray();
        params.add(uuid.toString());
        return params;
    }

    private JsonArray makeSelectParams(String email) {
        JsonArray params = new JsonArray();
        params.add(email);
        return params;
    }
}
