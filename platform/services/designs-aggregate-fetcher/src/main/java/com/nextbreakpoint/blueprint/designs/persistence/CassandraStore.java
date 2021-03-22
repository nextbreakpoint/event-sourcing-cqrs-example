package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.*;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.cassandra.CassandraClient;
import rx.Single;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CassandraStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(CassandraStore.class.getName());

    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private static final String SELECT_DESIGN = "SELECT * FROM DESIGN_ENTITY WHERE DESIGN_UUID = ?";
    private static final String SELECT_DESIGNS = "SELECT DESIGN_UUID, DESIGN_CHECKSUM, DESIGN_UPDATED FROM DESIGN_ENTITY";

    private final Supplier<CassandraClient> supplier;

    private CassandraClient session;

    private Single<PreparedStatement> selectDesign;
    private Single<PreparedStatement> selectDesigns;

    public CassandraStore(Supplier<CassandraClient> supplier) {
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

    private Single<CassandraClient> withSession() {
        if (session == null) {
            session = supplier.get();
            if (session == null) {
                return Single.error(new RuntimeException("Cannot create session"));
            }
            selectDesign = session.rxPrepare(SELECT_DESIGN);
            selectDesigns = session.rxPrepare(SELECT_DESIGNS);
        }
        return Single.just(session);
    }

    private Single<LoadDesignResponse> doLoadDesign(CassandraClient session, LoadDesignRequest request) {
        return selectDesign
                .map(pst -> pst.bind(makeLoadParams(request)))
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().findFirst().map(this::toDesignDocument).orElse(null))
                .map(document -> new LoadDesignResponse(request.getUuid(), document));
    }

    private Single<ListDesignsResponse> doListDesigns(CassandraClient session, ListDesignsRequest request) {
        return selectDesigns
                .map(PreparedStatement::bind)
                .flatMap(session::rxExecuteWithFullFetch)
                .map(rows -> rows.stream().map(this::toDesignDocumentWithoutData).collect(Collectors.toList()))
                .map(ListDesignsResponse::new);
    }

    private DesignDocument toDesignDocument(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String json = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Instant timestamp = row.getInstant("DESIGN_UPDATED");
        return new DesignDocument(Objects.requireNonNull(uuid).toString(), json, checksum, formatDate(timestamp));
    }

    private DesignDocument toDesignDocumentWithoutData(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final Instant timestamp = row.getInstant("DESIGN_UPDATED");
        return new DesignDocument(Objects.requireNonNull(uuid).toString(), null, checksum, formatDate(timestamp));
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
