package com.nextbreakpoint.blueprint.designs.model;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Design {
    private UUID uuid;
    private UUID evid;
    private String json;
    private String status;
    private String checksum;
    private Date updated;

    public Design(
        UUID uuid,
        UUID evid,
        String json,
        String status,
        String checksum,
        Date updated
    ) {
        this.uuid = uuid;
        this.evid = evid;
        this.json = json;
        this.status = status;
        this.checksum = checksum;
        this.updated = updated;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEvid() {
        return evid;
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

    public Date getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Design that = (Design) o;
        return Objects.equals(getUuid(), that.getUuid()) &&
                Objects.equals(getEvid(), that.getEvid()) &&
                Objects.equals(getJson(), that.getJson()) &&
                Objects.equals(getStatus(), that.getStatus()) &&
                Objects.equals(getChecksum(), that.getChecksum()) &&
                Objects.equals(getUpdated(), that.getUpdated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), getEvid(), getJson(), getStatus(), getChecksum(), getUpdated());
    }

    @Override
    public String toString() {
        return "DesignChange{" +
                "uuid='" + uuid + '\'' +
                ", evid='" + evid + '\'' +
                ", json='" + json + '\'' +
                ", status='" + status + '\'' +
                ", checksum='" + checksum + '\'' +
                ", updated='" + updated + '\'' +
                '}';
    }
}
