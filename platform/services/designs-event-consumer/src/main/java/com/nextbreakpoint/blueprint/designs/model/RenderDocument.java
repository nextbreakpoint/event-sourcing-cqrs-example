package com.nextbreakpoint.blueprint.designs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class RenderDocument {
    private final String uuid;
    private final String version;
    private final Short level;
    private final String created;
    private final String updated;

    @JsonCreator
    public RenderDocument(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("version") String version,
            @JsonProperty("level") Short level,
            @JsonProperty("created") String created,
            @JsonProperty("updated") String updated
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.version = Objects.requireNonNull(version);
        this.level = Objects.requireNonNull(level);
        this.created = Objects.requireNonNull(created);
        this.updated = updated;
    }

    public String getUuid() {
        return uuid;
    }

    public String getVersion() {
        return version;
    }

    public Short getLevel() {
        return level;
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
        RenderDocument that = (RenderDocument) o;
        return Objects.equals(getUuid(), that.getUuid()) && Objects.equals(getVersion(), that.getVersion()) && Objects.equals(getLevel(), that.getLevel()) && Objects.equals(getCreated(), that.getCreated()) && Objects.equals(getUpdated(), that.getUpdated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), getVersion(), getLevel(), getCreated(), getUpdated());
    }

    @Override
    public String toString() {
        return "RenderDocument{" +
                "uuid=" + uuid +
                ", version=" + version +
                ", level=" + level +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                '}';
    }
}
