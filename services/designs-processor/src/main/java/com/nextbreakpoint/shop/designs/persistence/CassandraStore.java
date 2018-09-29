package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.common.model.commands.DeleteDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.InsertDesignCommand;
import com.nextbreakpoint.shop.common.model.commands.UpdateDesignCommand;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.PersistenceResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting a design";
    private static final String ERROR_UPDATE_DESIGN = "An error occurred while updating a design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting a design";

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (DESIGN_UUID, DESIGN_JSON, DESIGN_STATUS, DESIGN_CHECKSUM, EVENT_TIMESTAMP) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_DESIGN_VIEW = "INSERT INTO DESIGNS_VIEW (DESIGN_UUID, DESIGN_JSON, DESIGN_CHECKSUM, DESIGN_TIMESTAMP) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_DESIGN_VIEW = "UPDATE DESIGNS_VIEW SET DESIGN_JSON=?, DESIGN_CHECKSUM=?, DESIGN_TIMESTAMP=? WHERE DESIGN_UUID=?";
    private static final String DELETE_DESIGN_VIEW = "DELETE FROM DESIGNS_VIEW WHERE DESIGN_UUID=?";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Supplier<Session> supplier;

    private Session session;

    private ListenableFuture<PreparedStatement> insertDesign;
    private ListenableFuture<PreparedStatement> insertDesignView;
    private ListenableFuture<PreparedStatement> updateDesignView;
    private ListenableFuture<PreparedStatement> deleteDesignView;

    public CassandraStore(Supplier<Session> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
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

    @Override
    public Single<PersistenceResult> insertDesignView(InsertDesignCommand command) {
        return withSession()
                .flatMap(session -> doInsertDesignView(session, command))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult> updateDesignView(UpdateDesignCommand command) {
        return withSession()
                .flatMap(conn -> doUpdateDesignView(session, command))
                .doOnError(err -> handleError(ERROR_UPDATE_DESIGN, err));
    }

    @Override
    public Single<PersistenceResult> deleteDesignView(DeleteDesignCommand command) {
        return withSession()
                .flatMap(session -> doDeleteDesignView(session, command))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    private Single<Session> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            insertDesign = session.prepareAsync(INSERT_DESIGN);
            insertDesignView = session.prepareAsync(INSERT_DESIGN_VIEW);
            updateDesignView = session.prepareAsync(UPDATE_DESIGN_VIEW);
            deleteDesignView = session.prepareAsync(DELETE_DESIGN_VIEW);
        }
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
                .map(rs -> {
                    if (!rs.wasApplied()) {
                        throw new RuntimeException("Cannot insert a design");
                    }
                    return new PersistenceResult(command.getUuid(), 1);
                });
    }

    private Single<PersistenceResult> doUpdateDesign(Session session, UpdateDesignCommand command) {
        return Single.from(insertDesign)
                .map(pst -> pst.bind(makeUpdateParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> {
                    if (!rs.wasApplied()) {
                        throw new RuntimeException("Cannot update a design");
                    }
                    return new PersistenceResult(command.getUuid(), 1);
                });
    }

    private Single<PersistenceResult> doDeleteDesign(Session session, DeleteDesignCommand command) {
        return Single.from(insertDesign)
                .map(pst -> pst.bind(makeDeleteParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> {
                    if (!rs.wasApplied()) {
                        throw new RuntimeException("Cannot delete a design");
                    }
                    return new PersistenceResult(command.getUuid(), 1);
                });
    }

    private Single<PersistenceResult> doInsertDesignView(Session session, InsertDesignCommand command) {
        return Single.from(insertDesignView)
                .map(pst -> pst.bind(makeInsertViewParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> {
                    if (!rs.wasApplied()) {
                        throw new RuntimeException("Cannot insert a design");
                    }
                    return new PersistenceResult(command.getUuid(), 1);
                });
    }

    private Single<PersistenceResult> doUpdateDesignView(Session session, UpdateDesignCommand command) {
        return Single.from(updateDesignView)
                .map(pst -> pst.bind(makeUpdateViewParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> {
                    if (!rs.wasApplied()) {
                        throw new RuntimeException("Cannot update a design");
                    }
                    return new PersistenceResult(command.getUuid(), 1);
                });
    }

    private Single<PersistenceResult> doDeleteDesignView(Session session, DeleteDesignCommand command) {
        return Single.from(deleteDesignView)
                .map(pst -> pst.bind(makeDeleteViewParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> {
                    if (!rs.wasApplied()) {
                        throw new RuntimeException("Cannot delete a design");
                    }
                    return new PersistenceResult(command.getUuid(), 1);
                });
    }

    private Object[] makeInsertParams(InsertDesignCommand event) {
        return new Object[] { event.getUuid(), event.getJson(), "CREATED", computeChecksum(event.getJson()), event.getTimestamp() };
    }

    private Object[] makeUpdateParams(UpdateDesignCommand event) {
        return new Object[] { event.getUuid(), event.getJson(), "UPDATED", computeChecksum(event.getJson()), event.getTimestamp() };
    }

    private Object[] makeDeleteParams(DeleteDesignCommand event) {
        return new Object[] { event.getUuid(), null, "DELETED", null, event.getTimestamp() };
    }

    private Object[] makeInsertViewParams(InsertDesignCommand event) {
        return new Object[] { event.getUuid(), event.getJson(), computeChecksum(event.getJson()), new Date(UUIDs.unixTimestamp(event.getTimestamp())) };
    }

    private Object[] makeUpdateViewParams(UpdateDesignCommand event) {
        return new Object[] { event.getJson(), computeChecksum(event.getJson()), new Date(UUIDs.unixTimestamp(event.getTimestamp())), event.getUuid() };
    }

    private Object[] makeDeleteViewParams(DeleteDesignCommand event) {
        return new Object[] { event.getUuid() };
    }

    private String computeChecksum(String json) {
        try {
            final byte[] bytes = json.getBytes("UTF-8");
            final MessageDigest md = MessageDigest.getInstance("MD5");
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute checksum", e);
        }
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
