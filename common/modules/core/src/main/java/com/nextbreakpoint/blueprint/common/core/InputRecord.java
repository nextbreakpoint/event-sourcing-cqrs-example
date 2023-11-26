package com.nextbreakpoint.blueprint.common.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(setterPrefix = "with")
public class InputRecord<T> {
    private final String topicName;
    private final String key;
    private final MessagePayload<T> payloadV2;
    private final List<Header> headers;
    private final int partition;
    private final long offset;
    private final long timestamp;
}
