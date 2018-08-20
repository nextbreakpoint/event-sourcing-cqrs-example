package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.model.GetStatusRequest;
import com.nextbreakpoint.shop.designs.model.GetStatusResponse;
import com.nextbreakpoint.shop.designs.model.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.model.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.ListStatusRequest;
import com.nextbreakpoint.shop.designs.model.ListStatusResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import com.nextbreakpoint.shop.designs.model.Design;
import com.nextbreakpoint.shop.designs.model.Status;
import com.nextbreakpoint.shop.designs.model.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nextbreakpoint.shop.common.vertx.TimeUtil.TIMESTAMP_PATTERN;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_DELETE_DESIGNS = "An error occurred while deleting all designs";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";
    private static final String ERROR_GET_DESIGNS_STATUS = "An error occurred while loading designs status";
    private static final String ERROR_GET_DESIGN_STATUS = "An error occurred while loading design status";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED) VALUES (?, ?, toTimeStamp(now()), toTimeStamp(now()))";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, UPDATED=toTimeStamp(now()) WHERE UUID=?";
    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGNS = "TRUNCATE DESIGNS";
    private static final String SELECT_DESIGNS = "SELECT * FROM DESIGNS";
    private static final String SELECT_STATUS = "SELECT NAME,MODIFIED FROM STATE WHERE NAME = 'designs'";
    private static final String UPDATE_STATUS = "UPDATE STATE SET MODIFIED = toTimeStamp(now()) WHERE NAME = 'designs'";
    private static final String FIND_DESIGNS = "SELECT UUID,UPDATED FROM DESIGNS WHERE UUID IN ($params)";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Session session;

    private final ListenableFuture<PreparedStatement> updateStatus;
    private final ListenableFuture<PreparedStatement> insertDesign;
    private final ListenableFuture<PreparedStatement> updateDesign;
    private final ListenableFuture<PreparedStatement> selectDesign;
    private final ListenableFuture<PreparedStatement> deleteDesign;
    private final ListenableFuture<PreparedStatement> deleteDesigns;
    private final ListenableFuture<PreparedStatement> selectDesigns;
    private final ListenableFuture<PreparedStatement> selectStatus;
    private final Map<Integer, ListenableFuture<PreparedStatement>> selectDesignsByUUIDS = new HashMap<>();

    public CassandraStore(Session session) {
        this.session = Objects.requireNonNull(session);
        updateStatus = session.prepareAsync(UPDATE_STATUS);
        insertDesign = session.prepareAsync(INSERT_DESIGN);
        updateDesign = session.prepareAsync(UPDATE_DESIGN);
        selectDesign = session.prepareAsync(SELECT_DESIGN);
        deleteDesign = session.prepareAsync(DELETE_DESIGN);
        deleteDesigns = session.prepareAsync(DELETE_DESIGNS);
        selectDesigns = session.prepareAsync(SELECT_DESIGNS);
        selectStatus = session.prepareAsync(SELECT_STATUS);
    }

    @Override
    public Single<InsertDesignResponse> insertDesign(InsertDesignRequest request) {
        return withSession()
                .flatMap(session -> doInsertDesign(session, request))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<UpdateDesignResponse> updateDesign(UpdateDesignRequest request) {
        return withSession()
                .flatMap(conn -> doUpdateDesign(session, request))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<LoadDesignResponse> loadDesign(LoadDesignRequest request) {
        return withSession()
                .flatMap(session -> doLoadDesign(session, request))
                .doOnError(err -> handleError(ERROR_LOAD_DESIGN, err));
    }

    @Override
    public Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request) {
        return withSession()
                .flatMap(session -> doDeleteDesign(session, request))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<DeleteDesignsResponse> deleteDesigns(DeleteDesignsRequest request) {
        return withSession()
                .flatMap(session -> doDeleteDesigns(session, request))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGNS, err));
    }

    @Override
    public Single<ListDesignsResponse> listDesigns(ListDesignsRequest request) {
        return withSession()
                .flatMap(session -> doListDesigns(session, request))
                .doOnError(err -> handleError(ERROR_LIST_DESIGNS, err));
    }

    @Override
    public Single<GetStatusResponse> getStatus(GetStatusRequest request) {
        return withSession()
                .flatMap(conn -> doGetStatus(session, request))
                .doOnError(err -> handleError(ERROR_GET_DESIGN_STATUS, err));
    }

    @Override
    public Single<ListStatusResponse> listStatus(ListStatusRequest request) {
        return withSession()
                .flatMap(conn -> doListStatus(session, request))
                .doOnError(err -> handleError(ERROR_GET_DESIGNS_STATUS, err));
    }

    private Single<Session> withSession() {
        return Single.just(session);
    }

    private Single<ResultSet> getResultSet(ResultSetFuture rsf) {
        return Single.fromCallable(() -> rsf.getUninterruptibly(EXECUTE_TIMEOUT, TimeUnit.SECONDS));
    }

    private Single<InsertDesignResponse> doInsertDesign(Session session, InsertDesignRequest request) {
        return Single.from(updateStatus)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .flatMap(x -> Single.from(insertDesign))
                .map(pst -> pst.bind(makeInsertParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new InsertDesignResponse(request.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<UpdateDesignResponse> doUpdateDesign(Session session, UpdateDesignRequest request) {
        return Single.from(updateStatus)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .flatMap(x -> Single.from(updateDesign))
                .map(pst -> pst.bind(makeUpdateParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new UpdateDesignResponse(request.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<LoadDesignResponse> doLoadDesign(Session session, LoadDesignRequest request) {
        return Single.from(selectDesign)
                .map(pst -> pst.bind(makeLoadParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> Optional.ofNullable(rs.one()))
                .map(result -> result.map(row -> {
                    final String uuid = row.getUUID("UUID").toString();
                    final String json = row.getString("JSON");
                    final long created = row.getTimestamp("CREATED").getTime();
                    final long updated = row.getTimestamp("UPDATED").getTime();
                    final Design design = new Design(uuid, json, formatDate(new Date(created)), formatDate(new Date(updated)), updated);
                    return new LoadDesignResponse(request.getUuid(), design);
                }).orElseGet(() -> new LoadDesignResponse(request.getUuid(), null)));
    }

    private Single<DeleteDesignResponse> doDeleteDesign(Session session, DeleteDesignRequest request) {
        return Single.from(updateStatus)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .flatMap(x -> Single.from(deleteDesign))
                .map(pst -> pst.bind(makeDeleteParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new DeleteDesignResponse(request.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<DeleteDesignsResponse> doDeleteDesigns(Session session, DeleteDesignsRequest request) {
        return Single.from(updateStatus)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .flatMap(x -> Single.from(deleteDesigns))
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new DeleteDesignsResponse(rs.wasApplied() ? 1 : 0));
    }

    private Single<ListDesignsResponse> doListDesigns(Session session, ListDesignsRequest request) {
        return Single.from(selectDesigns)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> rs.all())
                .map(result -> {
                    final Long updated = result
                            .stream()
                            .findFirst()
                            .map(json -> json.getTimestamp("UPDATED").getTime())
                            .orElse(0L);
                    final List<String> uuids = result
                            .stream()
                            .map(x -> x.getUUID("UUID").toString())
                            .collect(Collectors.toList());
                    return new ListDesignsResponse(updated, uuids);
                });
    }

    private Single<GetStatusResponse> doGetStatus(Session session, GetStatusRequest request) {
        return Single.from(selectStatus)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> Optional.ofNullable(rs.one()))
                .map(result -> {
                    final Long updated = result
                            .map(row -> row.getTimestamp("MODIFIED").getTime())
                            .orElse(0L);
                    return new GetStatusResponse(new Status("designs", updated));
                });
    }

    private Single<ListStatusResponse> doListStatus(Session session, ListStatusRequest request) {
        final ListenableFuture<PreparedStatement> selectDesigns = selectDesignsByUUIDS.computeIfAbsent(request.getUuids().size(), size -> session.prepareAsync(FIND_DESIGNS.replace("$params", makeParams(size))));
        return Single.from(selectDesigns)
                .map(pst -> pst.bind(makeListStatusParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> rs.all())
                .map(result -> {
                    final List<Status> list = result.stream()
                            .map(row -> {
                                final String uuid = row.getUUID("UUID").toString();
                                final long updated = row.getTimestamp("UPDATED").getTime();
                                return new Status(uuid, updated);
                            })
                            .collect(Collectors.toList());
                    return new ListStatusResponse(list);
                });
    }

    private Object[] makeInsertParams(InsertDesignRequest request) {
        return new Object[] { request.getUuid(), request.getJson() };
    }

    private Object[] makeUpdateParams(UpdateDesignRequest request) {
        return new Object[] { request.getJson(), request.getUuid() };
    }

    private Object[] makeDeleteParams(DeleteDesignRequest request) {
        return new Object[] { request.getUuid() };
    }

    private Object[] makeLoadParams(LoadDesignRequest request) {
        return new Object[] { request.getUuid() };
    }

    private Object[] makeListStatusParams(ListStatusRequest request) {
        return request.getUuids().stream().map(UUID::fromString).collect(Collectors.toList()).toArray();
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private String makeParams(int size) {
        return IntStream.range(0, size).mapToObj(i -> "?").collect(Collectors.joining(","));
    }

    private String formatDate(Date value) {
        final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);
        return df.format(value);
    }
}
