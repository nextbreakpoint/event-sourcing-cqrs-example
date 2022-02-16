package com.nextbreakpoint.blueprint.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
public class Tracing {
    private final UUID traceId;
    private final UUID spanId;
    private final UUID parent;

    @JsonCreator
    public Tracing(
        @JsonProperty("traceId") UUID traceId,
        @JsonProperty("spanId") UUID spanId,
        @JsonProperty("parent") UUID parent
    ) {
        this.traceId = Objects.requireNonNull(traceId);
        this.spanId = Objects.requireNonNull(spanId);
        this.parent = parent;
    }

    public static Tracing from(Tracing metadata) {
        return Tracing.builder()
                .withTraceId(metadata.getTraceId())
                .withSpanId(UUID.randomUUID())
                .withParent(metadata.getSpanId())
                .build();
    }

    public static Tracing of(UUID parent) {
        return Tracing.builder()
                .withTraceId(UUID.randomUUID())
                .withSpanId(UUID.randomUUID())
                .withParent(parent)
                .build();
    }

    public static Tracing from(Map<String, String> headers) {
        UUID traceId = Optional.ofNullable(headers.get("X-TRACE-TRACE-ID"))
                .map(UUID::fromString).orElse(UUID.randomUUID());

        UUID spanId = Optional.ofNullable(headers.get("X-TRACE-SPAN-ID"))
                .map(UUID::fromString).orElse(UUID.randomUUID());

        UUID parent = Optional.ofNullable(headers.get("X-TRACE-PARENT"))
                .map(UUID::fromString).orElse(null);

        return Tracing.builder()
                .withTraceId(traceId)
                .withSpanId(spanId)
                .withParent(parent)
                .build();
    }

    public Map<String, String> toHeaders() {
        if (getParent() != null) {
            return Map.of(
                    "X-TRACE-TRACE-ID", getTraceId().toString(),
                    "X-TRACE-SPAN-ID", getSpanId().toString(),
                    "X-TRACE-PARENT", getParent().toString()
            );
        } else {
            return Map.of(
                    "X-TRACE-TRACE-ID", getTraceId().toString(),
                    "X-TRACE-SPAN-ID", getSpanId().toString()
            );
        }
    }
}
