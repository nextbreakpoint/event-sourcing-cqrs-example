package com.nextbreakpoint.blueprint.designs.persistence;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import rx.Observable;
import rx.Single;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class ElasticsearchStore implements Store {
    private final Logger logger = LoggerFactory.getLogger(ElasticsearchStore.class.getName());

    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting design";

    private final ElasticsearchAsyncClient client;

    private final String indexName;

    public ElasticsearchStore(ElasticsearchAsyncClient client, String indexName) {
        this.client = Objects.requireNonNull(client);
        this.indexName = Objects.requireNonNull(indexName);;
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

    @Override
    public Single<InsertDesignResponse> insertDesign(InsertDesignRequest request) {
        return withHttpClient()
                .flatMap(client -> doInsertDesign(client, request))
                .doOnError(err -> handleError(ERROR_INSERT_DESIGN, err));
    }

    private Single<ElasticsearchAsyncClient> withHttpClient() {
        return Single.just(client);
    }

    private Single<LoadDesignResponse> doLoadDesign(ElasticsearchAsyncClient client, LoadDesignRequest request) {
        try {
            return Observable.from(client.search(builder -> createLoadDesignRequest(builder, request), Design.class))
                    .flatMap(search -> Observable.from(search.hits().hits()))
                    .map(Hit::source)
                    .limit(1)
                    .toList()
                    .map(results -> results.stream().findFirst().orElse(null))
                    .map(LoadDesignResponse::new)
                    .toSingle();
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    private Single<ListDesignsResponse> doListDesigns(ElasticsearchAsyncClient client, ListDesignsRequest request) {
        try {
            return Observable.from(client.search(builder -> createListDesignsRequest(builder, request), Design.class))
                    .flatMap(search -> Observable.from(search.hits().hits()))
                    .map(Hit::source)
                    .toList()
                    .map(ListDesignsResponse::new)
                    .toSingle();
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    private Single<InsertDesignResponse> doInsertDesign(ElasticsearchAsyncClient client, InsertDesignRequest request) {
        try {
            return Observable.from(client.<Design, Design>update(builder -> createInsertDesignRequest(builder, request), Design.class))
                    .flatMap(update -> Observable.just(Optional.ofNullable(update.get()).map(InlineGet::source)))
                    .map(design -> new InsertDesignResponse())
                    .toSingle();
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    private SearchRequest.Builder createLoadDesignRequest(SearchRequest.Builder builder, LoadDesignRequest request) {
        return builder
                .index(indexName)
                .query(q -> q.term(t -> t.field("uuid").value(v -> v.stringValue(request.getUuid().toString()))));
    }

    private SearchRequest.Builder createListDesignsRequest(SearchRequest.Builder builder, ListDesignsRequest request) {
        return builder
                .index(indexName)
                .query(q -> q.matchAll(MatchAllQuery.of(a -> a)))
                .sort(x -> x.field(f -> f.field("modified").order(SortOrder.Asc).format("strict_date_optional_time_nanos")) );
    }

    private UpdateRequest.Builder<Design, Design> createInsertDesignRequest(UpdateRequest.Builder<Design, Design> builder, InsertDesignRequest request) {
        return builder
                .index(indexName)
                .id(request.getDesign().getUuid().toString())
                .doc(request.getDesign())
                .docAsUpsert(true);
    }

    private void handleError(String message, Throwable err) {
        logger.error(message, err);
    }
}
