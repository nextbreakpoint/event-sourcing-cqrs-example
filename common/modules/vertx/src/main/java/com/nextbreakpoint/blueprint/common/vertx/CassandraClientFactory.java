package com.nextbreakpoint.blueprint.common.vertx;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;

import java.net.InetSocketAddress;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CassandraClientFactory {
    private CassandraClientFactory() {}

    public static CassandraClient create(Vertx vertx, CassandraClientConfig clientConfig) {
        final CqlSessionBuilder sessionBuilder = new CassandraClientOptions()
                .setKeyspace(clientConfig.getKeyspace())
                .dataStaxClusterBuilder()
                .withLocalDatacenter(clientConfig.getClusterName())
                .withAuthCredentials(clientConfig.getUsername(), clientConfig.getPassword(), "")
                .addContactPoints(Stream.of(clientConfig.getContactPoints()).map(contactPoint -> new InetSocketAddress(contactPoint, clientConfig.getPort())).collect(Collectors.toList()));

        return CassandraClient.createShared(vertx, clientConfig.getClusterName(), new CassandraClientOptions(sessionBuilder));
    }
}
