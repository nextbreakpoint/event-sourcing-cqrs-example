package com.nextbreakpoint.blueprint.designs;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.nextbreakpoint.blueprint.designs.model.Design;
import rx.Observable;

import java.util.List;
import java.util.UUID;

public class TestElasticsearch {
    private ElasticsearchClient client;
    private String indexName;

    public TestElasticsearch(ElasticsearchClient client, String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    public List<Design> findDesigns(UUID designId) {
        try {
            final SearchResponse<Design> response = client.search(builder -> createLoadDesignRequest(builder, designId), Design.class);

            final List<Design> designs = Observable.from(response.hits().hits())
                    .map(Hit::source)
                    .toList()
                    .toBlocking()
                    .firstOrDefault(List.of());

            return designs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteDesigns() {
        try {
            client.deleteByQuery(this::createDeleteDesignsRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insertDesign(Design design) {
        try {
            client.<Design, Design>update(builder -> createInsertDesignRequest(builder, design), Design.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SearchRequest.Builder createLoadDesignRequest(SearchRequest.Builder builder, UUID designId) {
        return builder
                .index(indexName)
                .query(q -> q.term(t -> t.field("designId").value(v -> v.stringValue(designId.toString()))));
    }

    private DeleteByQueryRequest.Builder createDeleteDesignsRequest(DeleteByQueryRequest.Builder builder) {
        return builder
                .index(indexName)
                .refresh(Boolean.TRUE)
                .query(new Query(MatchAllQuery.of(queryBuilder -> queryBuilder)));
    }

    private UpdateRequest.Builder<Design, Design> createInsertDesignRequest(UpdateRequest.Builder<Design, Design> builder, Design design) {
        return builder
                .index(indexName)
                .refresh(Refresh.True)
                .id(design.getDesignId().toString())
                .doc(design)
                .docAsUpsert(true);
    }
}
