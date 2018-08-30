package com.nextbreakpoint.shop.accounts.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.accounts.Store;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.accounts.model.Account;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_ACCOUNT = "An error occurred while inserting an account";
    private static final String ERROR_LOAD_ACCOUNT = "An error occurred while loading an account";
    private static final String ERROR_DELETE_ACCOUNT = "An error occurred while deleting an account";
    private static final String ERROR_FIND_ACCOUNTS = "An error occurred while loading accounts";

    private static final String INSERT_ACCOUNT = "INSERT INTO ACCOUNTS (UUID, NAME, EMAIL, ROLE, CREATED) VALUES (?, ?, ?, ?, toTimeStamp(now()))";
    private static final String SELECT_ACCOUNT = "SELECT * FROM ACCOUNTS WHERE UUID = ?";
    private static final String DELETE_ACCOUNT = "DELETE FROM ACCOUNTS WHERE UUID = ?";
    private static final String SELECT_ACCOUNTS = "SELECT * FROM ACCOUNTS";
    private static final String SELECT_ACCOUNTS_BY_EMAIL = "SELECT * FROM ACCOUNTS WHERE EMAIL = ?";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Session session;

    private final ListenableFuture<PreparedStatement> insertAccount;
    private final ListenableFuture<PreparedStatement> selectAccount;
    private final ListenableFuture<PreparedStatement> deleteAccount;
    private final ListenableFuture<PreparedStatement> selectAccountByEmail;
    private final ListenableFuture<PreparedStatement> selectAccounts;

    public CassandraStore(Session session) {
        this.session = Objects.requireNonNull(session);
        insertAccount = session.prepareAsync(INSERT_ACCOUNT);
        selectAccount = session.prepareAsync(SELECT_ACCOUNT);
        deleteAccount = session.prepareAsync(DELETE_ACCOUNT);
        selectAccountByEmail = session.prepareAsync(SELECT_ACCOUNTS_BY_EMAIL);
        selectAccounts = session.prepareAsync(SELECT_ACCOUNTS);
    }

    @Override
    public Single<InsertAccountResponse> insertAccount(InsertAccountRequest request) {
        return withSession()
                .flatMap(session -> doInsertAccount(session, request))
                .doOnError(err -> handleError(ERROR_INSERT_ACCOUNT, err));
    }

    @Override
    public Single<LoadAccountResponse> loadAccount(LoadAccountRequest request) {
        return withSession()
                .flatMap(session -> doLoadAccount(session, request))
                .doOnError(err -> handleError(ERROR_LOAD_ACCOUNT, err));
    }

    @Override
    public Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request) {
        return withSession()
                .flatMap(session -> doDeleteAccount(session, request))
                .doOnError(err -> handleError(ERROR_DELETE_ACCOUNT, err));
    }

    @Override
    public Single<ListAccountsResponse> listAccounts(ListAccountsRequest request) {
        return withSession()
                .flatMap(session -> doListAccounts(session, request))
                .doOnError(err -> handleError(ERROR_FIND_ACCOUNTS, err));
    }

    private Single<Session> withSession() {
        return Single.just(session);
    }

    private Single<ResultSet> getResultSet(ResultSetFuture rsf) {
        return Single.fromCallable(() -> rsf.getUninterruptibly(EXECUTE_TIMEOUT, TimeUnit.SECONDS));
    }

    private Single<InsertAccountResponse> doInsertAccount(Session session, InsertAccountRequest request) {
        return Single.from(insertAccount)
                .map(pst -> pst.bind(makeInsertParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new InsertAccountResponse(request.getUuid(), request.getRole(), rs.wasApplied() ? 1 : 0));
    }

    private Single<LoadAccountResponse> doLoadAccount(Session session, LoadAccountRequest request) {
        return Single.from(selectAccount)
                .map(pst -> pst.bind(makeLoadParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> Optional.ofNullable(rs.one()))
                .map(result -> result.map(row -> {
                    final String uuid = row.getUUID("UUID").toString();
                    final String name = row.getString("NAME");
                    final String role = row.getString("ROLE");
                    final Account account = new Account(uuid, name, role);
                    return new LoadAccountResponse(request.getUuid(), account);
                }).orElseGet(() -> new LoadAccountResponse(request.getUuid(), null)));
    }

    private Single<DeleteAccountResponse> doDeleteAccount(Session session, DeleteAccountRequest request) {
        return Single.from(deleteAccount)
                .map(pst -> pst.bind(makeDeleteParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new DeleteAccountResponse(request.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<ListAccountsResponse> doListAccounts(Session session, ListAccountsRequest request) {
        return selectAccounts(session, request)
                .map(ResultSet::all)
                .map(rows -> {
                    final List<String> uuids = rows
                            .stream()
                            .map(x -> x.getUUID("UUID").toString())
                            .collect(Collectors.toList());
                    return new ListAccountsResponse(uuids);
                });
    }

    private Single<ResultSet> selectAccounts(Session session, ListAccountsRequest request) {
        if (request.getEmail().isPresent()) {
            return Single.from(selectAccountByEmail)
                    .map(pst -> pst.bind(makeListParams(request)))
                    .map(bst -> session.executeAsync(bst))
                    .flatMap(rsf -> getResultSet(rsf));
        } else {
            return Single.from(selectAccounts)
                    .map(pst -> pst.bind())
                    .map(bst -> session.executeAsync(bst))
                    .flatMap(rsf -> getResultSet(rsf));
        }
    }

    private Object[] makeInsertParams(InsertAccountRequest request) {
        return new Object[] { request.getUuid(), request.getName(), request.getEmail(), request.getRole() };
    }

    private Object[] makeDeleteParams(DeleteAccountRequest request) {
        return new Object[] { request.getUuid() };
    }

    private Object[] makeLoadParams(LoadAccountRequest request) {
        return new Object[] { request.getUuid() };
    }

    private Object[] makeListParams(ListAccountsRequest request) {
        return request.getEmail().map(email -> new Object[] { email }).orElseGet(() -> new Object[0]);
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
