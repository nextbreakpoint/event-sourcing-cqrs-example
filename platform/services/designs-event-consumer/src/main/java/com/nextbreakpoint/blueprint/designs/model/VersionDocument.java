package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class VersionDocument {
    private String uuid;
    private String json;
    private String checksum;
    private String created;
    private String updated;

    @JsonCreator
    public VersionDocument(@JsonProperty("uuid") String uuid,
                           @JsonProperty("json") String json,
                           @JsonProperty("checksum") String checksum,
                           @JsonProperty("created") String created,
                           @JsonProperty("updated") String updated) {
        this.uuid = uuid;
        this.json = json;
        this.checksum = checksum;
        this.created = created;
        this.updated = updated;
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

    public String getCreated() {
        return created;
    }

    public String getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionDocument that = (VersionDocument) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(json, that.json) && Objects.equals(checksum, that.checksum) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, json, checksum, created, updated);
    }

    @Override
    public String toString() {
        return "VersionDocument{" +
                "uuid='" + uuid + '\'' +
                ", json='" + json + '\'' +
                ", checksum='" + checksum + '\'' +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                '}';
    }
}
