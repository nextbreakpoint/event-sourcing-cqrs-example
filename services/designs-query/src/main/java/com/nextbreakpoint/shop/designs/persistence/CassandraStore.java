package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.common.cassandra.CassandraClusterFactory;
import com.nextbreakpoint.shop.common.model.DesignDocument;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS WHERE UUID = ?";
    private static final String SELECT_DESIGNS = "SELECT * FROM DESIGNS";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Supplier<Session> supplier;

    private Session session;

    private ListenableFuture<PreparedStatement> selectDesign;
    private ListenableFuture<PreparedStatement> selectDesigns;

    public CassandraStore(Supplier<Session> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public Single<LoadDesignResponse> loadDesign(LoadDesignRequest request) {
        return withSession()
                .flatMap(session -> doLoadDesign(session, request))
                .doOnError(err -> handleError(ERROR_LOAD_DESIGN, err));
    }

    @Override
    public Single<ListDesignsResponse> listDesigns(ListDesignsRequest request) {
        return withSession()
                .flatMap(session -> doListDesigns(session, request))
                .doOnError(err -> handleError(ERROR_LIST_DESIGNS, err));
    }

    private Single<Session> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            selectDesign = session.prepareAsync(SELECT_DESIGN);
            selectDesigns = session.prepareAsync(SELECT_DESIGNS);
        }
        return Single.just(session);
    }

    private Single<ResultSet> getResultSet(ResultSetFuture rsf) {
        return Single.fromCallable(() -> rsf.getUninterruptibly(EXECUTE_TIMEOUT, TimeUnit.SECONDS));
    }

    private Single<LoadDesignResponse> doLoadDesign(Session session, LoadDesignRequest request) {
        return Single.from(selectDesign)
                .map(pst -> pst.bind(makeLoadParams(request)))
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> rs.all())
                .map(result -> result.stream()
                        .map(row -> {
                            final String uuid = row.getUUID("UUID").toString();
                            final String json = row.getString("JSON");
                            final String status = row.getString("STATUS");
                            final String checksum = row.getString("CHECKSUM");
                            final Instant timestamp = row.getTimestamp("EVENTTIME").toInstant();
                            return new DesignDocument(uuid, json, status, checksum, formatDate(timestamp));
                        })
                        .reduce(this::reduceEvents)
                )
                .map(document -> new LoadDesignResponse(request.getUuid(), document.orElse(null)));
    }

    private DesignDocument reduceEvents(DesignDocument designDocument1, DesignDocument designDocument2) {
        if (designDocument2.getStatus().equalsIgnoreCase("deleted")) {
            return new DesignDocument(designDocument1.getUuid(), designDocument1.getJson(), designDocument2.getStatus(), designDocument2.getModified(), designDocument1.getChecksum());
        } else {
            return new DesignDocument(designDocument1.getUuid(), designDocument2.getJson(), designDocument2.getStatus(), designDocument2.getModified(), designDocument2.getChecksum());
        }
    }

    private Single<ListDesignsResponse> doListDesigns(Session session, ListDesignsRequest request) {
        return Single.from(selectDesigns)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> rs.all())
                .map(result -> result.stream()
                        .map(row -> {
                            final String uuid = row.getUUID("UUID").toString();
                            final String checksum = row.getString("CHECKSUM");
                            return new DesignDocument(uuid, null, null, checksum, null);
                        })
                        .collect(Collectors.toList()))
                .map(documents -> new ListDesignsResponse(documents));
    }

    private Object[] makeLoadParams(LoadDesignRequest request) {
        return new Object[] { request.getUuid() };
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
