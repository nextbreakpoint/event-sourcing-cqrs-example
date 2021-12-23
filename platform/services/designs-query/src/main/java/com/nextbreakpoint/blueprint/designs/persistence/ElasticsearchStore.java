package com.nextbreakpoint.blueprint.designs.persistence;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.operations.list.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.core.http.HttpClient;
import rx.Single;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class ElasticsearchStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(ElasticsearchStore.class.getName());

    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";

    private final HttpClient httpClient;

    public ElasticsearchStore(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public Single<LoadDesignResponse> loadDesign(LoadDesignRequest request) {
        return withHttpClient()
                .flatMap(client -> doLoadDesign(client, request))
                .doOnError(err -> handleError(ERROR_LOAD_DESIGN, err));
    }

    @Override
    public Single<ListDesignsResponse> listDesigns(ListDesignsRequest request) {
        return withHttpClient()
                .flatMap(client -> doListDesigns(client, request))
                .doOnError(err -> handleError(ERROR_LIST_DESIGNS, err));
    }

    private Single<HttpClient> withHttpClient() {
        return Single.just(httpClient);
    }

    private Single<LoadDesignResponse> doLoadDesign(HttpClient client, LoadDesignRequest request) {
        return null;
    }

    private Single<ListDesignsResponse> doListDesigns(HttpClient client, ListDesignsRequest request) {
        return null;
    }

    private boolean isNotDeleted(DesignDocument designDocument) {
        return !designDocument.getStatus().equals("DELETED");
    }

    private DesignDocument toDesignDocument(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String json = row.getString("DESIGN_DATA");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final String status = row.getString("DESIGN_STATUS");
        final Instant timestamp = row.getInstant("DESIGN_UPDATED");
        return new DesignDocument(Objects.requireNonNull(uuid).toString(), json, checksum, status, formatDate(timestamp));
    }

    private DesignDocument toDesignDocumentWithoutData(Row row) {
        final UUID uuid = row.getUuid("DESIGN_UUID");
        final String checksum = row.getString("DESIGN_CHECKSUM");
        final String status = row.getString("DESIGN_STATUS");
        final Instant timestamp = row.getInstant("DESIGN_UPDATED");
        return new DesignDocument(Objects.requireNonNull(uuid).toString(), null, checksum, status, formatDate(timestamp));
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
