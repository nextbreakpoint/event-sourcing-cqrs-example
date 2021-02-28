package com.nextbreakpoint.blueprint.accounts.persistence;

import com.nextbreakpoint.blueprint.accounts.Store;
import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.blueprint.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.blueprint.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.blueprint.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.blueprint.common.core.Account;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.TransactionIsolation;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class MySQLStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(MySQLStore.class.getName());

    private static final SQLOptions OPTIONS = new SQLOptions()
            .setTransactionIsolation(TransactionIsolation.SERIALIZABLE)
            .setQueryTimeout(10000);

    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_ACCOUNT = "An error occurred while inserting an account";
    private static final String ERROR_LOAD_ACCOUNT = "An error occurred while loading an account";
    private static final String ERROR_DELETE_ACCOUNT = "An error occurred while deleting an account";
    private static final String ERROR_FIND_ACCOUNTS = "An error occurred while loading accounts";

    private static final String INSERT_ACCOUNT = "INSERT INTO ACCOUNTS (UUID, NAME, EMAIL, ROLE) VALUES (?, ?, ?, ?)";
    private static final String SELECT_ACCOUNT = "SELECT * FROM ACCOUNTS WHERE UUID = ?";
    private static final String DELETE_ACCOUNT = "DELETE FROM ACCOUNTS WHERE UUID = ?";
    private static final String SELECT_ACCOUNTS = "SELECT * FROM ACCOUNTS";
    private static final String SELECT_ACCOUNTS_BY_EMAIL = "SELECT * FROM ACCOUNTS WHERE EMAIL = ?";

    private final JDBCClient client;

    public MySQLStore(JDBCClient client) {
        this.client = client;
    }

    public Single<InsertAccountResponse> insertAccount(InsertAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doInsertAccount(conn, request))
                .doOnError(err -> handleError(ERROR_INSERT_ACCOUNT, err));
    }

    public Single<LoadAccountResponse> loadAccount(LoadAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doLoadAccount(conn, request))
                .doOnError(err -> handleError(ERROR_LOAD_ACCOUNT, err));
    }

    public Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteAccount(conn, request))
                .doOnError(err -> handleError(ERROR_DELETE_ACCOUNT, err));
    }

    public Single<ListAccountsResponse> listAccounts(ListAccountsRequest request) {
        return withConnection()
                .flatMap(conn -> doListAccounts(conn, request))
                .doOnError(err -> handleError(ERROR_FIND_ACCOUNTS, err));
    }

    private Single<SQLConnection> withConnection() {
        return client.rxGetConnection()
                .doOnSuccess(conn -> conn.setOptions(OPTIONS))
                .doOnError(err -> handleError(ERROR_GET_CONNECTION, err));
    }

    private Single<InsertAccountResponse> doInsertAccount(SQLConnection conn, InsertAccountRequest request) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdateWithParams(INSERT_ACCOUNT, makeInsertParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new InsertAccountResponse(request.getUuid(), request.getRole(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<LoadAccountResponse> doLoadAccount(SQLConnection conn, LoadAccountRequest request) {
        return conn.rxSetAutoCommit(true)
                .flatMap(x -> conn.rxQueryWithParams(SELECT_ACCOUNT, makeLoadParams(request)))
                .map(ResultSet::getRows)
                .map(this::exactlyOne)
                .map(result -> result.map(this::toAccount).orElse(null))
                .map(account -> new LoadAccountResponse(request.getUuid(), account))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteAccountResponse> doDeleteAccount(SQLConnection conn, DeleteAccountRequest request) {
        return conn.rxSetAutoCommit(false)
                .flatMap(x -> conn.rxUpdateWithParams(DELETE_ACCOUNT, makeDeleteParams(request)))
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteAccountResponse(request.getUuid(), result))
                .doOnError(x -> conn.rxRollback().subscribe())
                .doOnSuccess(x -> conn.rxCommit().subscribe())
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListAccountsResponse> doListAccounts(SQLConnection conn, ListAccountsRequest request) {
        return conn.rxSetAutoCommit(true)
                .flatMap(x -> selectAccounts(conn, request))
                .map(ResultSet::getRows)
                .map(result -> result.stream().map(x -> x.getString("UUID")).collect(toList()))
                .map(ListAccountsResponse::new)
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ResultSet> selectAccounts(SQLConnection conn, ListAccountsRequest request) {
        if (request.getEmail().isPresent()) {
            return conn.rxQueryWithParams(SELECT_ACCOUNTS_BY_EMAIL, makeListParams(request));
        } else {
            return conn.rxQuery(SELECT_ACCOUNTS);
        }
    }

    private Account toAccount(JsonObject row) {
        final String uuid = row.getString("UUID");
        final String name = row.getString("NAME");
        final String role = row.getString("ROLE");
        return new Account(uuid, name, role);
    }

    private Optional<JsonObject> exactlyOne(List<JsonObject> list) {
        return Optional.ofNullable(list).filter(rows -> rows.size() == 1).map(rows -> rows.get(0));
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private JsonArray makeInsertParams(InsertAccountRequest request) {
        return new JsonArray()
                .add(request.getUuid().toString())
                .add(request.getName())
                .add(request.getEmail())
                .add(request.getRole());
    }

    private JsonArray makeDeleteParams(DeleteAccountRequest request) {
        return new JsonArray().add(request.getUuid().toString());
    }

    private JsonArray makeLoadParams(LoadAccountRequest request) {
        return new JsonArray().add(request.getUuid().toString());
    }

    private JsonArray makeListParams(ListAccountsRequest request) {
        final JsonArray params = new JsonArray();
        if (request.getEmail().isPresent()) {
            params.add(request.getEmail().get());
        }
        return params;
    }
}
