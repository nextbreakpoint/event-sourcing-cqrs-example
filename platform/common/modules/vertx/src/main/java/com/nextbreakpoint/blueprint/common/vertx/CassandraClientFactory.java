package com.nextbreakpoint.blueprint.common.vertx;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;

import java.util.stream.Stream;

public class CassandraClientFactory {
    private CassandraClientFactory() {}

    public static CassandraClient create(Vertx vertx, JsonObject config) {
        final String clusterName = config.getString("cassandra_cluster");
        final String keyspace = config.getString("cassandra_keyspace");
        final String username = config.getString("cassandra_username");
        final String password = config.getString("cassandra_password");
        final String[] contactPoints = config.getString("cassandra_contactPoints").split(",");
        final int port = Integer.parseInt(config.getString("cassandra_port", "9042"));

        final CqlSessionBuilder sessionBuilder = new CassandraClientOptions()
                .setKeyspace(keyspace)
                .dataStaxClusterBuilder()
                .withLocalDatacenter(clusterName)
                .withAuthCredentials(username, password, "");

        final CassandraClientOptions options = new CassandraClientOptions(sessionBuilder);

        Stream.of(contactPoints).forEach(contactPoint -> options.addContactPoint(contactPoint, port));

        return CassandraClient.createShared(vertx, clusterName, options);
    }
}
