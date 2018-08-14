package com.nextbreakpoint.shop.designs;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignResponse;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsRequest;
import com.nextbreakpoint.shop.designs.delete.DeleteDesignsResponse;
import com.nextbreakpoint.shop.designs.insert.InsertDesignRequest;
import com.nextbreakpoint.shop.designs.insert.InsertDesignResponse;
import com.nextbreakpoint.shop.designs.update.UpdateDesignRequest;
import com.nextbreakpoint.shop.designs.update.UpdateDesignResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;

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
    private final ListenableFuture<PreparedStatement> deleteDesign;
    private final ListenableFuture<PreparedStatement> deleteDesigns;
    private final Map<Integer, ListenableFuture<PreparedStatement>> selectDesignsByUUIDS = new HashMap<>();

    public CassandraStore(Session session) {
        this.session = Objects.requireNonNull(session);
        updateStatus = session.prepareAsync(UPDATE_STATUS);
        insertDesign = session.prepareAsync(INSERT_DESIGN);
        updateDesign = session.prepareAsync(UPDATE_DESIGN);
        deleteDesign = session.prepareAsync(DELETE_DESIGN);
        deleteDesigns = session.prepareAsync(DELETE_DESIGNS);
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

    private Object[] makeInsertParams(InsertDesignRequest request) {
        return new Object[] { request.getUuid(), request.getJson() };
    }

    private Object[] makeUpdateParams(UpdateDesignRequest request) {
        return new Object[] { request.getJson(), request.getUuid() };
    }

    private Object[] makeDeleteParams(DeleteDesignRequest request) {
        return new Object[] { request.getUuid() };
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
