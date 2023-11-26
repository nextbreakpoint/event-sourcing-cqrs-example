package com.nextbreakpoint.blueprint.common.drivers;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true, access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaProducerConfig {
    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final String keySerializer;
    private final String valueSerializer;
    private final String keystoreLocation;
    private final String keystorePassword;
    private final String truststoreLocation;
    private final String truststorePassword;
    private final String schemaRegistryKeystoreLocation;
    private final String schemaRegistryKeystorePassword;
    private final String schemaRegistryTruststoreLocation;
    private final String schemaRegistryTruststorePassword;
    private final String clientId;
    private final String kafkaAcks;
    private final Boolean autoRegisterSchemas;
}
