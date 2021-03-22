package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class DesignChange {
    private UUID uuid;
    private String json;
    private String status;
    private String checksum;
    private Date modified;

    @JsonCreator
    public DesignChange(@JsonProperty("uuid") UUID uuid,
                        @JsonProperty("json") String json,
                        @JsonProperty("status") String status,
                        @JsonProperty("checksum") String checksum,
                        @JsonProperty("modified") Date modified) {
        this.uuid = uuid;
        this.json = json;
        this.status = status;
        this.checksum = checksum;
        this.modified = modified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesignChange that = (DesignChange) o;
        return Objects.equals(getUuid(), that.getUuid()) &&
                Objects.equals(getJson(), that.getJson()) &&
                Objects.equals(getStatus(), that.getStatus()) &&
                Objects.equals(getChecksum(), that.getChecksum()) &&
                Objects.equals(getModified(), that.getModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), getJson(), getStatus(), getChecksum(), getModified());
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getJson() {
        return json;
    }

    public String getStatus() {
        return status;
    }

    public String getChecksum() {
        return checksum;
    }

    public Date getModified() {
        return modified;
    }

    @Override
    public String toString() {
        return "DesignChange{" +
                "uuid='" + uuid + '\'' +
                ", json='" + json + '\'' +
                ", status='" + status + '\'' +
                ", checksum='" + checksum + '\'' +
                ", modified='" + modified + '\'' +
                '}';
    }
}
