package com.nextbreakpoint.blueprint.designs.model;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class DesignChange {
    private UUID uuid;
    private String json;
    private String status;
    private String checksum;
    private Date modified;

    public DesignChange(
        UUID uuid,
        String json,
        String status,
        String checksum,
        Date modified
    ) {
        this.uuid = uuid;
        this.json = json;
        this.status = status;
        this.checksum = checksum;
        this.modified = modified;
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
