package com.nextbreakpoint.blueprint.common.vertx;

import com.datastax.driver.core.Cluster;
import io.vertx.core.json.JsonObject;
import com.nextbreakpoint.blueprint.common.core.Environment;

import java.util.stream.Stream;

public class CassandraClusterFactory {
    private CassandraClusterFactory() {}

    public static Cluster create(Environment environment, JsonObject config) {
        final String clusterName = environment.resolve(config.getString("cassandra_cluster"));
        final String username = environment.resolve(config.getString("cassandra_username"));
        final String password = environment.resolve(config.getString("cassandra_password"));
        final String[] contactPoints = environment.resolve(config.getString("cassandra_contactPoints")).split(",");
        final Integer port = Integer.parseInt(environment.resolve(config.getString("cassandra_port", "9042")));

        final Cluster.Builder builder = Cluster.builder()
                .withClusterName(clusterName)
                .withCredentials(username, password)
                .withPort(port);

        Stream.of(contactPoints).forEach(builder::addContactPoint);

        return builder.build();
    }
}
