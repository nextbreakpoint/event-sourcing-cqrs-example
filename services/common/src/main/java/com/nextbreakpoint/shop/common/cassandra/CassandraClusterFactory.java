package com.nextbreakpoint.shop.common.cassandra;

import com.datastax.driver.core.Cluster;
import io.vertx.core.json.JsonObject;

public class CassandraClusterFactory {
    private CassandraClusterFactory() {}

    public static Cluster create(JsonObject config) {
        final String clusterName = config.getString("cassandra_cluster");
        final String username = config.getString("cassandra_username");
        final String password = config.getString("cassandra_password");
        final String contactPoint = config.getString("cassandra_contactPoint");
        final Integer port = config.getInteger("cassandra_port");

        return Cluster.builder()
                .withClusterName(clusterName)
                .addContactPoint(contactPoint)
                .withCredentials(username, password)
                .withPort(port)
                .build();
    }
}
