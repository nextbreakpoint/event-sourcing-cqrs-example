package com.nextbreakpoint.shop.common.cassandra;

import com.datastax.driver.core.Cluster;
import io.vertx.core.json.JsonObject;

import java.util.stream.Stream;

public class CassandraClusterFactory {
    private CassandraClusterFactory() {}

    public static Cluster create(JsonObject config) {
        final String clusterName = config.getString("cassandra_cluster");
        final String username = config.getString("cassandra_username");
        final String password = config.getString("cassandra_password");
        final String[] contactPoints = config.getString("cassandra_contactPoints").split(",");
        final Integer port = config.getInteger("cassandra_port");

        final Cluster.Builder builder = Cluster.builder()
                .withClusterName(clusterName)
                .withCredentials(username, password)
                .withPort(port);

        Stream.of(contactPoints).forEach(builder::addContactPoint);

        return builder.build();
    }
}
