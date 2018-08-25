package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignEvent;
import com.nextbreakpoint.shop.common.model.events.DeleteDesignsEvent;
import com.nextbreakpoint.shop.common.model.events.InsertDesignEvent;
import com.nextbreakpoint.shop.common.model.events.UpdateDesignEvent;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.DeleteDesignResult;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResult;
import com.nextbreakpoint.shop.designs.model.InsertDesignResult;
import com.nextbreakpoint.shop.designs.model.UpdateDesignResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";
    private static final String ERROR_DELETE_DESIGNS = "An error occurred while deleting all designs";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED) VALUES (?, ?, toTimeStamp(now()), toTimeStamp(now()))";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, UPDATED=toTimeStamp(now()) WHERE UUID=?";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGNS WHERE UUID = ?";
    private static final String DELETE_DESIGNS = "TRUNCATE DESIGNS";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Session session;

    private final ListenableFuture<PreparedStatement> insertDesign;
    private final ListenableFuture<PreparedStatement> updateDesign;
    private final ListenableFuture<PreparedStatement> deleteDesign;
    private final ListenableFuture<PreparedStatement> deleteDesigns;

    public CassandraStore(Session session) {
        this.session = Objects.requireNonNull(session);
        insertDesign = session.prepareAsync(INSERT_DESIGN);
        updateDesign = session.prepareAsync(UPDATE_DESIGN);
        deleteDesign = session.prepareAsync(DELETE_DESIGN);
        deleteDesigns = session.prepareAsync(DELETE_DESIGNS);
    }

    @Override
    public Single<InsertDesignResult> insertDesign(InsertDesignEvent event) {
        return withSession()
                .flatMap(session -> doInsertDesign(session, event))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<UpdateDesignResult> updateDesign(UpdateDesignEvent event) {
        return withSession()
                .flatMap(conn -> doUpdateDesign(session, event))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<DeleteDesignResult> deleteDesign(DeleteDesignEvent event) {
        return withSession()
                .flatMap(session -> doDeleteDesign(session, event))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<DeleteDesignsResult> deleteDesigns(DeleteDesignsEvent event) {
        return withSession()
                .flatMap(session -> doDeleteDesigns(session, event))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGNS, err));
    }

    private Single<Session> withSession() {
        return Single.just(session);
    }

    private Single<ResultSet> getResultSet(ResultSetFuture future) {
        return Single.fromCallable(() -> future.getUninterruptibly(EXECUTE_TIMEOUT, TimeUnit.SECONDS));
    }

    private Single<InsertDesignResult> doInsertDesign(Session session, InsertDesignEvent event) {
        return Single.from(insertDesign)
                .map(pst -> pst.bind(makeInsertParams(event)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new InsertDesignResult(event.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<UpdateDesignResult> doUpdateDesign(Session session, UpdateDesignEvent event) {
        return Single.from(updateDesign)
                .map(pst -> pst.bind(makeUpdateParams(event)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new UpdateDesignResult(event.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<DeleteDesignResult> doDeleteDesign(Session session, DeleteDesignEvent event) {
        return Single.from(deleteDesign)
                .map(pst -> pst.bind(makeDeleteParams(event)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new DeleteDesignResult(event.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<DeleteDesignsResult> doDeleteDesigns(Session session, DeleteDesignsEvent event) {
        return Single.from(deleteDesigns)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new DeleteDesignsResult(rs.wasApplied() ? 1 : 0));
    }

    private Object[] makeInsertParams(InsertDesignEvent event) {
        return new Object[] { event.getUuid(), event.getJson() };
    }

    private Object[] makeUpdateParams(UpdateDesignEvent event) {
        return new Object[] { event.getJson(), event.getUuid() };
    }

    private Object[] makeDeleteParams(DeleteDesignEvent event) {
        return new Object[] { event.getUuid() };
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
