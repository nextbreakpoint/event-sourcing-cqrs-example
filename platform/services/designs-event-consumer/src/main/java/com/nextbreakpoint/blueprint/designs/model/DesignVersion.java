package com.nextbreakpoint.blueprint.designs.model;

import java.util.Objects;

public class DesignVersion {
    private final String checksum;
    private final String data;

    public DesignVersion(
        String checksum,
        String data
    ) {
        this.checksum = Objects.requireNonNull(checksum);
        this.data = Objects.requireNonNull(data);
    }

    public String getChecksum() {
        return checksum;
    }

    public String getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DesignVersion that = (DesignVersion) o;
        return Objects.equals(getChecksum(), that.getChecksum()) && Objects.equals(getData(), that.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChecksum(), getData());
    }

    @Override
    public String toString() {
        return "DesignVersion{" +
                "checksum='" + checksum + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
