package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.PersistenceResult;
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

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, CREATED, UPDATED) VALUES (?, ?, toTimeStamp(now()), toTimeStamp(now()))";
    private static final String UPDATE_DESIGN = "UPDATE DESIGNS SET JSON=?, UPDATED=toTimeStamp(now()) WHERE UUID=?";
    private static final String DELETE_DESIGN = "DELETE FROM DESIGNS WHERE UUID = ?";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Session session;

    private final ListenableFuture<PreparedStatement> insertDesign;
    private final ListenableFuture<PreparedStatement> updateDesign;
    private final ListenableFuture<PreparedStatement> deleteDesign;

    public CassandraStore(Session session) {
        this.session = Objects.requireNonNull(session);
        insertDesign = session.prepareAsync(INSERT_DESIGN);
        updateDesign = session.prepareAsync(UPDATE_DESIGN);
        deleteDesign = session.prepareAsync(DELETE_DESIGN);
    }

    @Override
    public Single<PersistenceResult> insertDesign(InsertDesignCommand command) {
        return withSession()
                .flatMap(session -> doInsertDesign(session, command))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult> updateDesign(UpdateDesignCommand command) {
        return withSession()
                .flatMap(conn -> doUpdateDesign(session, command))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult> deleteDesign(DeleteDesignCommand command) {
        return withSession()
                .flatMap(session -> doDeleteDesign(session, command))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    private Single<Session> withSession() {
        return Single.just(session);
    }

    private Single<ResultSet> getResultSet(ResultSetFuture future) {
        return Single.fromCallable(() -> future.getUninterruptibly(EXECUTE_TIMEOUT, TimeUnit.SECONDS));
    }

    private Single<PersistenceResult> doInsertDesign(Session session, InsertDesignCommand command) {
        return Single.from(insertDesign)
                .map(pst -> pst.bind(makeInsertParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new PersistenceResult(command.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<PersistenceResult> doUpdateDesign(Session session, UpdateDesignCommand command) {
        return Single.from(updateDesign)
                .map(pst -> pst.bind(makeUpdateParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new PersistenceResult(command.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<PersistenceResult> doDeleteDesign(Session session, DeleteDesignCommand command) {
        return Single.from(deleteDesign)
                .map(pst -> pst.bind(makeDeleteParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new PersistenceResult(command.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Object[] makeInsertParams(InsertDesignCommand event) {
        return new Object[] { event.getUuid(), event.getJson() };
    }

    private Object[] makeUpdateParams(UpdateDesignCommand event) {
        return new Object[] { event.getJson(), event.getUuid() };
    }

    private Object[] makeDeleteParams(DeleteDesignCommand event) {
        return new Object[] { event.getUuid() };
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
