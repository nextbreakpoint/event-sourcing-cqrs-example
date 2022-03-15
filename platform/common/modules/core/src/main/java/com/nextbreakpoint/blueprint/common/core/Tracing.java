package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tracing {
    private final String traceId;
    private final String spanId;

    @JsonCreator
    public Tracing(
        @JsonProperty("traceId") String traceId,
        @JsonProperty("spanId") String spanId
    ) {
        this.traceId = Objects.requireNonNull(traceId);
        this.spanId = Objects.requireNonNull(spanId);
    }

    public static Tracing of(String traceId, String spanId) {
        return Tracing.builder()
                .withTraceId(traceId)
                .withSpanId(spanId)
                .build();
    }

    public static Tracing from(Map<String, String> headers) {
        return new Tracing(headers.get("X-TRACE-ID"), headers.get("X-SPAN-ID"));
    }

    public Map<String, String> toHeaders() {
        return Map.of("X-TRACE-ID", traceId, "X-SPAN-ID", spanId);
    }
}
