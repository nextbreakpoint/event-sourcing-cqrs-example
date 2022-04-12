package com.nextbreakpoint.blueprint.common.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Checksum {
    private Checksum() {}

    public static String of(String data) {
        if (data == null) {
            return null;
        }
        return of(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String of(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(bytes);
            final StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%x00d", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute checksum", e);
        }
    }
}
