package com.nextbreakpoint.shop.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DesignDocument {
    private String uuid;
    private String json;
    private String created;
    private String updated;
    private Long modified;

    @JsonCreator
    public DesignDocument(@JsonProperty("uuid") String uuid,
                          @JsonProperty("json") String json,
                          @JsonProperty("created") String created,
                          @JsonProperty("updated") String updated,
                          @JsonProperty("modified") Long modified) {
        this.uuid = uuid;
        this.json = json;
        this.created = created;
        this.updated = updated;
        this.modified = modified;
    }

    public String getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }

    public String getCreated() {
        return created;
    }

    public String getUpdated() {
        return updated;
    }

    public Long getModified() {
        return modified;
    }
}
