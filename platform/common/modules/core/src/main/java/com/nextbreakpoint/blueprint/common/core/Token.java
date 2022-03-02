package com.nextbreakpoint.blueprint.common.core;

public class Token {
    private Token() {}

    public static String from(long timestamp, long offset) {
        return String.format("%016d-%016d", timestamp, offset);
    }
}
