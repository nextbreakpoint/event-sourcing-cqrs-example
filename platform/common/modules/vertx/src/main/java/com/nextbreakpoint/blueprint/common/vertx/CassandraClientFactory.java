package com.nextbreakpoint.blueprint.common.vertx;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;

import java.util.stream.Stream;

public class CassandraClientFactory {
    private CassandraClientFactory() {}

    public static CassandraClient create(Vertx vertx, CassandraClientConfig clientConfig) {
        final CqlSessionBuilder sessionBuilder = new CassandraClientOptions()
                .setKeyspace(clientConfig.getKeyspace())
                .dataStaxClusterBuilder()
                .withLocalDatacenter(clientConfig.getClusterName())
                .withAuthCredentials(clientConfig.getUsername(), clientConfig.getPassword(), "");

        final CassandraClientOptions options = new CassandraClientOptions(sessionBuilder);

        Stream.of(clientConfig.getContactPoints()).forEach(contactPoint -> options.addContactPoint(contactPoint, clientConfig.getPort()));

        return CassandraClient.createShared(vertx, clientConfig.getClusterName(), options);
    }
}
