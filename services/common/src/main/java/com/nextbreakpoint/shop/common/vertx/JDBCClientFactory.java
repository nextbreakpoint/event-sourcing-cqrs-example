package com.nextbreakpoint.shop.common.vertx;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;

public class JDBCClientFactory {
    private JDBCClientFactory() {}

    public static JDBCClient create(Vertx vertx, JsonObject config) {
        final String jdbcUrl = config.getString("jdbc_url", "jdbc:hsqldb:mem:test?shutdown=true");
        final String jdbcDriver = config.getString("jdbc_driver", "org.hsqldb.jdbcDriver");
        final String jdbcUsername = config.getString("jdbc_username", "root");
        final String jdbcPassword = config.getString("jdbc_password", "root");
        final int jdbcMaxPoolSize = config.getInteger("jdbc_max_pool_size", 200);
        final int jdbcMinPoolSize = config.getInteger("jdbc_min_pool_size", 20);

        final JsonObject jdbcConfig = new JsonObject()
                .put("url", jdbcUrl)
                .put("driver_class", jdbcDriver)
                .put("max_pool_size", jdbcMaxPoolSize)
                .put("min_pool_size", jdbcMinPoolSize)
                .put("user", jdbcUsername)
                .put("password", jdbcPassword);

        return JDBCClient.createShared(vertx, jdbcConfig);
    }
}
