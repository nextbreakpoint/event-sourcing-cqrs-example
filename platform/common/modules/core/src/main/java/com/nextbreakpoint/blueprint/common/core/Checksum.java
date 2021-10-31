package com.nextbreakpoint.blueprint.common.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class Checksum {
    private Checksum() {}

    public static String of(String data) {
        if (data == null) {
            return null;
        }
        try {
            final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            final MessageDigest md = MessageDigest.getInstance("MD5");
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute checksum", e);
        }
    }
}
