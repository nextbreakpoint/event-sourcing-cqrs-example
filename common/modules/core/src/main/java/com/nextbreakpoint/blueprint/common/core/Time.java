package com.nextbreakpoint.blueprint.common.core;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class Time {
    private Time() {}

    private static String toISOFormat(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
