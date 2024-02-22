package com.nextbreakpoint.blueprint.accounts.persistence;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.accounts.model.Account;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsRequest;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsResponse;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.TransactionIsolation;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Log4j2
public class MySQLStore implements Store {
    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_ACCOUNT = "An error occurred while inserting an account";
    private static final String ERROR_LOAD_ACCOUNT = "An error occurred while loading an account";
    private static final String ERROR_DELETE_ACCOUNT = "An error occurred while deleting an account";
    private static final String ERROR_FIND_ACCOUNTS = "An error occurred while loading accounts";

    private static final String INSERT_ACCOUNT = "INSERT INTO ACCOUNT (ACCOUNT_UUID, ACCOUNT_NAME, ACCOUNT_LOGIN, ACCOUNT_AUTHORITIES, ACCOUNT_CREATED) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
    private static final String SELECT_ACCOUNT = "SELECT ACCOUNT_UUID, ACCOUNT_NAME, ACCOUNT_LOGIN, ACCOUNT_AUTHORITIES, ACCOUNT_CREATED FROM ACCOUNT WHERE ACCOUNT_UUID = ?";
    private static final String DELETE_ACCOUNT = "DELETE FROM ACCOUNT WHERE ACCOUNT_UUID = ?";
    private static final String SELECT_ACCOUNTS = "SELECT ACCOUNT_UUID, ACCOUNT_NAME, ACCOUNT_LOGIN, ACCOUNT_AUTHORITIES, ACCOUNT_CREATED FROM ACCOUNT";
    private static final String SELECT_ACCOUNTS_BY_EMAIL = "SELECT ACCOUNT_UUID, ACCOUNT_NAME, ACCOUNT_LOGIN, ACCOUNT_AUTHORITIES, ACCOUNT_CREATED FROM ACCOUNT WHERE ACCOUNT_LOGIN = ?";

    private static final SQLOptions OPTIONS = new SQLOptions()
            .setTransactionIsolation(TransactionIsolation.READ_UNCOMMITTED)
            .setQueryTimeout(10000);

    private final JDBCClient client;

    public MySQLStore(JDBCClient client) {
        this.client = client;
    }

    public Single<InsertAccountResponse> insertAccount(InsertAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doInsertAccount(conn, request))
                .doOnError(err -> handleError(ERROR_INSERT_ACCOUNT, err));
    }

    public Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteAccount(conn, request))
                .doOnError(err -> handleError(ERROR_DELETE_ACCOUNT, err));
    }

    public Single<LoadAccountResponse> loadAccount(LoadAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doLoadAccount(conn, request))
                .doOnError(err -> handleError(ERROR_LOAD_ACCOUNT, err));
    }

    public Single<ListAccountsResponse> listAccounts(ListAccountsRequest request) {
        return withConnection()
                .flatMap(conn -> doListAccounts(conn, request))
                .doOnError(err -> handleError(ERROR_FIND_ACCOUNTS, err));
    }

    public Single<Boolean> existsTable(String tableName) {
        return withConnection()
                .flatMap(conn -> doExistsTable(conn, tableName));
    }

    private Single<SQLConnection> withConnection() {
        return client.rxGetConnection()
                .doOnSuccess(conn -> conn.setOptions(OPTIONS))
                .doOnError(err -> handleError(ERROR_GET_CONNECTION, err));
    }

    private Single<InsertAccountResponse> doInsertAccount(SQLConnection conn, InsertAccountRequest request) {
        return conn.rxSetTransactionIsolation(TransactionIsolation.READ_COMMITTED)
                .flatMap(ignore -> conn.rxSetAutoCommit(false))
                .flatMap(x -> conn.rxUpdateWithParams(INSERT_ACCOUNT, makeInsertParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new InsertAccountResponse(request.getUuid(), request.getAuthorities(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteAccountResponse> doDeleteAccount(SQLConnection conn, DeleteAccountRequest request) {
        return conn.rxSetTransactionIsolation(TransactionIsolation.READ_COMMITTED)
                .flatMap(ignore -> conn.rxSetAutoCommit(false))
                .flatMap(x -> conn.rxUpdateWithParams(DELETE_ACCOUNT, makeDeleteParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteAccountResponse(request.getUuid(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<LoadAccountResponse> doLoadAccount(SQLConnection conn, LoadAccountRequest request) {
        return conn.rxSetTransactionIsolation(TransactionIsolation.READ_COMMITTED)
                .flatMap(ignore -> conn.rxSetAutoCommit(false))
                .flatMap(x -> conn.rxQueryWithParams(SELECT_ACCOUNT, makeLoadParams(request)))
                .map(ResultSet::getRows)
                .map(this::exactlyOne)
                .map(result -> result.map(this::toAccount).orElse(null))
                .map(account -> new LoadAccountResponse(request.getUuid(), account))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListAccountsResponse> doListAccounts(SQLConnection conn, ListAccountsRequest request) {
        return conn.rxSetTransactionIsolation(TransactionIsolation.READ_COMMITTED)
                .flatMap(ignore -> conn.rxSetAutoCommit(false))
                .flatMap(x -> selectAccounts(conn, request))
                .map(ResultSet::getRows)
                .map(result -> result.stream().map(x -> x.getString("ACCOUNT_UUID")).collect(toList()))
                .map(ListAccountsResponse::new)
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<Boolean> doExistsTable(SQLConnection conn, String tableName) {
        return conn.rxSetTransactionIsolation(TransactionIsolation.READ_COMMITTED)
                .flatMap(ignore -> conn.rxSetAutoCommit(false))
                .flatMap(x -> conn.rxQuery("SELECT 1 FROM " + tableName))
                .map(result -> true)
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ResultSet> selectAccounts(SQLConnection conn, ListAccountsRequest request) {
        if (request.getLogin().isPresent()) {
            return conn.rxQueryWithParams(SELECT_ACCOUNTS_BY_EMAIL, makeListParams(request));
        } else {
            return conn.rxQuery(SELECT_ACCOUNTS);
        }
    }

    private Account toAccount(JsonObject row) {
        final String uuid = row.getString("ACCOUNT_UUID");
        final String name = row.getString("ACCOUNT_NAME");
        final String role = row.getString("ACCOUNT_AUTHORITIES");

        return Account.builder()
                .withUuid(uuid)
                .withName(name)
                .withAuthorities(role)
                .build();
    }

    private Optional<JsonObject> exactlyOne(List<JsonObject> list) {
        return Optional.ofNullable(list).filter(rows -> rows.size() == 1).map(rows -> rows.get(0));
    }

    private void handleError(String message, Throwable err) {
        log.error(message, err);
    }

    private JsonArray makeInsertParams(InsertAccountRequest request) {
        return new JsonArray()
                .add(request.getUuid().toString())
                .add(request.getName())
                .add(request.getLogin())
                .add(request.getAuthorities());
    }

    private JsonArray makeDeleteParams(DeleteAccountRequest request) {
        return new JsonArray().add(request.getUuid().toString());
    }

    private JsonArray makeLoadParams(LoadAccountRequest request) {
        return new JsonArray().add(request.getUuid().toString());
    }

    private JsonArray makeListParams(ListAccountsRequest request) {
        final JsonArray params = new JsonArray();
        if (request.getLogin().isPresent()) {
            params.add(request.getLogin().get());
        }
        return params;
    }
}
