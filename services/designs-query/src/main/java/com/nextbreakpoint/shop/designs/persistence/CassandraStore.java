package com.nextbreakpoint.shop.designs.persistence;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nextbreakpoint.shop.common.vertx.TimeUtil.TIMESTAMP_PATTERN;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGNS WHERE UUID = ?";
    private static final String SELECT_DESIGNS = "SELECT * FROM DESIGNS";

    private static final int EXECUTE_TIMEOUT = 10;

    private final Session session;

    private final ListenableFuture<PreparedStatement> selectDesign;
    private final ListenableFuture<PreparedStatement> selectDesigns;

    public CassandraStore(Session session) {
        this.session = Objects.requireNonNull(session);
        selectDesign = session.prepareAsync(SELECT_DESIGN);
        selectDesigns = session.prepareAsync(SELECT_DESIGNS);
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
                .map(rs -> Optional.ofNullable(rs.one()))
                .map(result -> result.map(row -> {
                    final String uuid = row.getUUID("UUID").toString();
                    final String json = row.getString("JSON");
                    final long created = row.getTimestamp("CREATED").getTime();
                    final long updated = row.getTimestamp("UPDATED").getTime();
                    final DesignDocument designDocument = new DesignDocument(uuid, json, formatDate(new Date(created)), formatDate(new Date(updated)), updated);
                    return new LoadDesignResponse(request.getUuid(), designDocument);
                }).orElseGet(() -> new LoadDesignResponse(request.getUuid(), null)));
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

    private Object[] makeLoadParams(LoadDesignRequest request) {
        return new Object[] { request.getUuid() };
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private String formatDate(Date value) {
        final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);
        return df.format(value);
    }
}
