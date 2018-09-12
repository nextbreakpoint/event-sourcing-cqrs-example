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

    private static final String INSERT_DESIGN = "INSERT INTO DESIGNS (UUID, JSON, STATUS, CHECKSUM, EVENTTIME) VALUES (?, ?, ?, ?, ?)";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Supplier<Session> supplier;

    private Session session;

    private ListenableFuture<PreparedStatement> insertDesign;

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

    private Single<Session> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            insertDesign = session.prepareAsync(INSERT_DESIGN);
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
                .map(rs -> new PersistenceResult(command.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<PersistenceResult> doUpdateDesign(Session session, UpdateDesignCommand command) {
        return Single.from(insertDesign)
                .map(pst -> pst.bind(makeUpdateParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new PersistenceResult(command.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Single<PersistenceResult> doDeleteDesign(Session session, DeleteDesignCommand command) {
        return Single.from(insertDesign)
                .map(pst -> pst.bind(makeDeleteParams(command)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> new PersistenceResult(command.getUuid(), rs.wasApplied() ? 1 : 0));
    }

    private Object[] makeInsertParams(InsertDesignCommand event) {
        return new Object[] { event.getUuid(), event.getJson(), "CREATED", computeChecksum(event.getJson()), new Date(event.getTimestamp())};
    }

    private Object[] makeUpdateParams(UpdateDesignCommand event) {
        return new Object[] { event.getUuid(), event.getJson(), "UPDATED", computeChecksum(event.getJson()), new Date(event.getTimestamp())};
    }

    private Object[] makeDeleteParams(DeleteDesignCommand event) {
        return new Object[] { event.getUuid(), null, "DELETED", null, new Date(event.getTimestamp())};
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
