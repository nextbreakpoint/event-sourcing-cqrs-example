package com.nextbreakpoint.blueprint.common.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.StreamSupport;

public final class Checksum {
    private Checksum() {}

    public static String of(String data) {
        if (data == null) {
            return null;
        }
        try {
            final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(bytes);
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                builder.append(String.format("%x00d", digest[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute checksum", e);
        }
    }
}
