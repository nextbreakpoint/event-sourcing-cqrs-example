package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListenableFuture;
import com.nextbreakpoint.shop.common.model.DesignDocument;
import com.nextbreakpoint.shop.designs.Store;
import com.nextbreakpoint.shop.designs.model.ListDesignsRequest;
import com.nextbreakpoint.shop.designs.model.ListDesignsResponse;
import com.nextbreakpoint.shop.designs.model.LoadDesignRequest;
import com.nextbreakpoint.shop.designs.model.LoadDesignResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS_VIEW WHERE DESIGN_UUID = ?";
    private static final String SELECT_DESIGNS = "SELECT DESIGN_UUID, DESIGN_CHECKSUM, DESIGN_TIMESTAMP FROM DESIGNS_VIEW";

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
                .map(rs -> {
                    final List<DesignDocument> documents = new ArrayList<>();
                    final Iterator<Row> iter = rs.iterator();
                    if (iter.hasNext()) {
                        if (rs.getAvailableWithoutFetching() >= 100 && !rs.isFullyFetched()) {
                            rs.fetchMoreResults();
                        }
                        final Row row = iter.next();
                        documents.add(getDesignDocument(row));
                    }
                    return documents;
                })
                .map(documents -> documents.stream().findFirst())
                .map(document -> new LoadDesignResponse(request.getUuid(), document.orElse(null)));
    }

    private DesignDocument getDesignDocument(Row row) {
        final String uuid = row.getUUID("DESIGN_UUID").toString();
        final String json = row.getString("DESIGN_JSON");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Instant timestamp = row.getTimestamp("DESIGN_TIMESTAMP").toInstant();
        return new DesignDocument(uuid, json, checksum, formatDate(timestamp));
    }

    private Single<ListDesignsResponse> doListDesigns(Session session, ListDesignsRequest request) {
        return Single.from(selectDesigns)
                .map(pst -> pst.bind())
                .map(bst -> session.executeAsync(bst))
                .flatMap(rsf -> getResultSet(rsf))
                .map(rs -> {
                    final List<DesignDocument> documents = new ArrayList<>();
                    final Iterator<Row> iter = rs.iterator();
                    while (iter.hasNext()) {
                        if (rs.getAvailableWithoutFetching() >= 100 && !rs.isFullyFetched()) {
                            rs.fetchMoreResults();
                        }
                        final Row row = iter.next();
                        documents.add(getMinimalDesignDocument(row));
                    }
                    return documents;
                })
                .map(documents -> new ListDesignsResponse(documents));
    }

    private DesignDocument getMinimalDesignDocument(Row row) {
        final String uuid = row.getUUID("DESIGN_UUID").toString();
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Instant timestamp = row.getTimestamp("DESIGN_TIMESTAMP").toInstant();
        return new DesignDocument(uuid, null, checksum, formatDate(timestamp));
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
