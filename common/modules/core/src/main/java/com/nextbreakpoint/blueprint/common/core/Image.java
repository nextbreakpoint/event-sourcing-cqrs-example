package com.nextbreakpoint.blueprint.common.core;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder(setterPrefix = "with")
public class Image {
    private final byte[] data;
    private final String checksum;

    public Image(byte[] data, String checksum) {
        this.data = Objects.requireNonNull(data);
        this.checksum = Objects.requireNonNull(checksum);;
    }
}
