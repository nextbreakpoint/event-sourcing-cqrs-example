package com.nextbreakpoint.shop.designs.model;

import com.nextbreakpoint.shop.common.model.DesignDocument;

import java.util.List;
import java.util.Objects;

public class ListDesignsResponse {
    private final List<DesignDocument> documents;

    public ListDesignsResponse(List<DesignDocument> documents) {
        this.documents = Objects.requireNonNull(documents);
    }

    public List<DesignDocument> getDocuments() {
        return documents;
    }
}
