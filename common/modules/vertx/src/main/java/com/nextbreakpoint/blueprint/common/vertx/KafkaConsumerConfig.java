package com.nextbreakpoint.blueprint.common.vertx;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true, access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaConsumerConfig {
    private final String bootstrapServers;
    private final String keyDeserializer;
    private final String valueDeserializer;
    private final String keystoreLocation;
    private final String keystorePassword;
    private final String truststoreLocation;
    private final String truststorePassword;
    private final String groupId;
    private final String autoOffsetReset;
    private final String enableAutoCommit;
}
