package com.nextbreakpoint.blueprint.designs.persistence;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.DeleteDesignResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.InsertDesignResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import lombok.extern.log4j.Log4j2;
import rx.Observable;
import rx.Single;

import java.util.Objects;
import java.util.Optional;

@Log4j2
public class ElasticsearchStore implements Store {
    private static final String ERROR_LOAD_DESIGN = "An error occurred while loading a design";
    private static final String ERROR_LIST_DESIGNS = "An error occurred while loading designs";
    private static final String ERROR_INSERT_DESIGN = "An error occurred while inserting design";
    private static final String ERROR_DELETE_DESIGN = "An error occurred while deleting design";

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

    @Override
    public Single<DeleteDesignResponse> deleteDesign(DeleteDesignRequest request) {
        return withHttpClient()
                .flatMap(client -> doDeleteDesign(client, request))
                .doOnError(err -> handleError(ERROR_DELETE_DESIGN, err));
    }

    @Override
    public Single<Boolean> existsIndex(String indexName) {
        return withHttpClient()
                .flatMap(client -> doExistsIndex(client, indexName));
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
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    private Single<ListDesignsResponse> doListDesigns(ElasticsearchAsyncClient client, ListDesignsRequest request) {
        try {
            return Observable.from(client.search(builder -> createListDesignsRequest(builder, request), Design.class))
                    .map(SearchResponse::hits)
                    .flatMap(hits -> Observable.from(hits.hits()).map(Hit::source).toList().map(designs -> new ListDesignsResponse(designs, hits.total().value())))
                    .toSingle();
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    private Single<InsertDesignResponse> doInsertDesign(ElasticsearchAsyncClient client, InsertDesignRequest request) {
        try {
            return Observable.from(client.<Design, Design>update(builder -> createInsertDesignRequest(builder, request), Design.class))
                    .flatMap(update -> Observable.just(Optional.ofNullable(update.get()).map(InlineGet::source)))
                    .map(design -> new InsertDesignResponse())
                    .toSingle();
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    private Single<DeleteDesignResponse> doDeleteDesign(ElasticsearchAsyncClient client, DeleteDesignRequest request) {
        try {
            return Observable.from(client.delete(builder -> createDeleteDesignRequest(builder, request)))
                    .map(delete -> new DeleteDesignResponse())
                    .toSingle();
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    private Single<Boolean> doExistsIndex(ElasticsearchAsyncClient client, String indexName) {
        try {
            return Observable.from(client.search(builder -> createExistsIndexRequest(builder, indexName), Design.class))
                    .map(result -> true)
                    .toSingle();
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    private SearchRequest.Builder createLoadDesignRequest(SearchRequest.Builder builder, LoadDesignRequest request) {
        return builder
                .index(getIndexName(request.isDraft()))
                .query(q -> q.term(t -> t.field("designId").value(v -> v.stringValue(request.getUuid().toString()))));
    }

    private SearchRequest.Builder createListDesignsRequest(SearchRequest.Builder builder, ListDesignsRequest request) {
        return builder
                .index(getIndexName(request.isDraft()))
                .query(q -> q.matchAll(MatchAllQuery.of(a -> a)))
                .from(request.getFrom())
                .size(request.getSize())
                .sort(x -> x.field(f -> f.field("created").order(SortOrder.Desc).format("strict_date_optional_time_nanos")) );
    }

    private SearchRequest.Builder createExistsIndexRequest(SearchRequest.Builder builder, String indexName) {
        return builder
                .index(indexName)
                .query(q -> q.matchAll(MatchAllQuery.of(a -> a)))
                .size(1);
    }

    private UpdateRequest.Builder<Design, Design> createInsertDesignRequest(UpdateRequest.Builder<Design, Design> builder, InsertDesignRequest request) {
        return builder
                .index(getIndexName(request.isDraft()))
                .refresh(Refresh.True)
                .id(request.getDesign().getDesignId().toString())
                .doc(request.getDesign())
                .docAsUpsert(true);
    }

    private DeleteRequest.Builder createDeleteDesignRequest(DeleteRequest.Builder builder, DeleteDesignRequest request) {
        return builder
                .index(getIndexName(request.isDraft()))
                .refresh(Refresh.True)
                .id(request.getUuid().toString());
    }

    private String getIndexName(boolean draft) {
        return draft ? indexName + "_draft" : indexName;
    }

    private void handleError(String message, Throwable err) {
        log.error(message, err);
    }
}
