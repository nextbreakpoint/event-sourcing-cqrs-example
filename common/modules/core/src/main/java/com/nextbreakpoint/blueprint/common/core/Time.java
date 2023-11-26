package com.nextbreakpoint.blueprint.common.core;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class Time {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(Clock.systemUTC().getZone());

    private Time() {}

    public static String format(Instant instant) {
        return FORMATTER.format(instant);
    }
}
