package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DesignDocument {
    private String uuid;
    private String json;
    private String checksum;
    private String modified;

    @JsonCreator
    public DesignDocument(@JsonProperty("uuid") String uuid,
                          @JsonProperty("json") String json,
                          @JsonProperty("checksum") String checksum,
                          @JsonProperty("modified") String modified) {
        this.uuid = uuid;
        this.json = json;
        this.checksum = checksum;
        this.modified = modified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesignDocument that = (DesignDocument) o;
        return Objects.equals(getUuid(), that.getUuid()) &&
                Objects.equals(getJson(), that.getJson()) &&
                Objects.equals(getChecksum(), that.getChecksum()) &&
                Objects.equals(getModified(), that.getModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), getJson(), getChecksum(), getModified());
    }

    public String getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getModified() {
        return modified;
    }

    @Override
    public String toString() {
        return "DesignDocument{" +
                "uuid='" + uuid + '\'' +
                ", json='" + json + '\'' +
                ", checksum='" + checksum + '\'' +
                ", modified='" + modified + '\'' +
                '}';
    }
}
