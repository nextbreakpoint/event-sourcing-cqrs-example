package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.accounts.delete.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsRequest;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsResponse;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.list.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.list.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.load.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.load.LoadAccountResponse;
import com.nextbreakpoint.shop.accounts.model.Account;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JDBCStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(JDBCStore.class.getName());

    private static final String ERROR_GET_CONNECTION = "An error occurred while getting a connection";
    private static final String ERROR_INSERT_ACCOUNT = "An error occurred while inserting an account";
    private static final String ERROR_LOAD_ACCOUNT = "An error occurred while loading an account";
    private static final String ERROR_DELETE_ACCOUNT = "An error occurred while deleting an account";
    private static final String ERROR_DELETE_ACCOUNTS = "An error occurred while deleting all accounts";
    private static final String ERROR_FIND_ACCOUNTS = "An error occurred while loading accounts";

    private static final String INSERT_ACCOUNT = "INSERT INTO ACCOUNTS (UUID, NAME, EMAIL, ROLE) VALUES (?, ?, ?, ?)";
    private static final String SELECT_ACCOUNT = "SELECT * FROM ACCOUNTS WHERE UUID = ?";
    private static final String DELETE_ACCOUNT = "DELETE FROM ACCOUNTS WHERE UUID = ?";
    private static final String DELETE_ACCOUNTS = "DELETE FROM ACCOUNTS";
    private static final String SELECT_ACCOUNTS = "SELECT * FROM ACCOUNTS";
    private static final String SELECT_ACCOUNTS_BY_EMAIL = "SELECT * FROM ACCOUNTS WHERE EMAIL = ?";

    private static final int EXECUTE_TIMEOUT = 10;
    private static final int CONNECT_TIMEOUT = 5;

    private final JDBCClient client;

    public JDBCStore(JDBCClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public Single<InsertAccountResponse> insertAccount(InsertAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doInsertAccount(conn, request))
                .doOnError(err -> handleError(ERROR_INSERT_ACCOUNT, err));
    }

    @Override
    public Single<LoadAccountResponse> loadAccount(LoadAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doLoadAccount(conn, request))
                .doOnError(err -> handleError(ERROR_LOAD_ACCOUNT, err));
    }

    @Override
    public Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteAccount(conn, request))
                .doOnError(err -> handleError(ERROR_DELETE_ACCOUNT, err));
    }

    @Override
    public Single<DeleteAccountsResponse> deleteAccounts(DeleteAccountsRequest request) {
        return withConnection()
                .flatMap(conn -> doDeleteAccounts(conn, request))
                .doOnError(err -> handleError(ERROR_DELETE_ACCOUNTS, err));
    }

    @Override
    public Single<ListAccountsResponse> listAccounts(ListAccountsRequest request) {
        return withConnection()
                .flatMap(conn -> doListAccounts(conn, request))
                .doOnError(err -> handleError(ERROR_FIND_ACCOUNTS, err));
    }

    private Single<SQLConnection> withConnection() {
        return client.rxGetConnection()
                .timeout(CONNECT_TIMEOUT, SECONDS)
                .doOnError(err -> handleError(ERROR_GET_CONNECTION, err));
    }

    private Single<InsertAccountResponse> doInsertAccount(SQLConnection conn, InsertAccountRequest request) {
        return conn.rxUpdateWithParams(INSERT_ACCOUNT, makeInsertParams(request))
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(UpdateResult::getUpdated)
                .map(result -> new InsertAccountResponse(request.getUuid(), request.getRole(), result))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<LoadAccountResponse> doLoadAccount(SQLConnection conn, LoadAccountRequest request) {
        return conn.rxQueryWithParams(SELECT_ACCOUNT, makeLoadParams(request))
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(this::exactlyOne)
                .map(result -> result.map(row -> {
                    final String uuid = row.getString("UUID");
                    final String name = row.getString("NAME");
                    final String role = row.getString("ROLE");
                    final Account account = new Account(uuid.toString(), name, role);
                    return new LoadAccountResponse(request.getUuid(), account);
                }).orElseGet(() -> new LoadAccountResponse(request.getUuid(), null)))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteAccountResponse> doDeleteAccount(SQLConnection conn, DeleteAccountRequest request) {
        return conn.rxUpdateWithParams(DELETE_ACCOUNT, makeDeleteParams(request))
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteAccountResponse(request.getUuid(), result))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<DeleteAccountsResponse> doDeleteAccounts(SQLConnection conn, DeleteAccountsRequest request) {
        return conn.rxUpdate(DELETE_ACCOUNTS)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(UpdateResult::getUpdated)
                .map(result -> new DeleteAccountsResponse(result))
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ListAccountsResponse> doListAccounts(SQLConnection conn, ListAccountsRequest request) {
        return selectAccounts(conn, request)
                .timeout(EXECUTE_TIMEOUT, SECONDS)
                .map(ResultSet::getRows)
                .map(result -> {
                    final List<String> list = result
                            .stream()
                            .map(x -> x.getString("UUID"))
                            .collect(Collectors.toList());
                    return new ListAccountsResponse(list);
                })
                .doAfterTerminate(() -> conn.rxClose().subscribe());
    }

    private Single<ResultSet> selectAccounts(SQLConnection conn, ListAccountsRequest request) {
        if (request.getEmail().isPresent()) {
            return conn.rxQueryWithParams(SELECT_ACCOUNTS_BY_EMAIL, makeListParams(request));
        } else {
            return conn.rxQuery(SELECT_ACCOUNTS);
        }
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
