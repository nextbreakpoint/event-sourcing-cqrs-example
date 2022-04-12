package com.nextbreakpoint.blueprint.common.core;

import java.util.Objects;

public class Image {
    private final byte[] data;
    private final String checksum;

    public Image(byte[] data, String checksum) {
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);;
    }

    public byte[] getData() {
        return data;
    }

    public String getChecksum() {
        return checksum;
    }
}
