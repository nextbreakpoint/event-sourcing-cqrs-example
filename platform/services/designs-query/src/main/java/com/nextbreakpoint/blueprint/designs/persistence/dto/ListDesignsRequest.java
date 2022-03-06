package com.nextbreakpoint.blueprint.designs.persistence.dto;

public class ListDesignsRequest {
    private boolean draft;

    public ListDesignsRequest(boolean draft) {
        this.draft = draft;
    }

    public boolean isDraft() {
        return draft;
    }
}
