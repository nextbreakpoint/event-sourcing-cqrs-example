package com.nextbreakpoint.blueprint.common.drivers;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.micrometer.core.instrument.MeterRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CassandraClientFactory {
    private CassandraClientFactory() {}

    public static CqlSession create(CassandraClientConfig clientConfig, MeterRegistry registry) {
        return new CqlSessionBuilder()
                .withKeyspace(clientConfig.getKeyspace())
                .withLocalDatacenter(clientConfig.getClusterName())
                .withAuthCredentials(clientConfig.getUsername(), clientConfig.getPassword(), "")
                .addContactPoints(getContactPoints(clientConfig))
                .withMetricRegistry(registry)
                .build();
    }

    public static CqlSession create(CassandraClientConfig clientConfig) {
        return create(clientConfig, null);
    }

    private static List<InetSocketAddress> getContactPoints(CassandraClientConfig clientConfig) {
        return Stream.of(clientConfig.getContactPoints())
                .map(contactPoint -> new InetSocketAddress(contactPoint, clientConfig.getPort()))
                .collect(Collectors.toList());
    }
}
