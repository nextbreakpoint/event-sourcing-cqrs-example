package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;

public class JDBCClientFactory {
    private JDBCClientFactory() {}

    public static JDBCClient create(Vertx vertx, JDBCClientConfig jdbcConfig) {
        final JsonObject object = new JsonObject()
                .put("url", jdbcConfig.getUrl())
                .put("driver_class", jdbcConfig.getDriver())
                .put("max_pool_size", jdbcConfig.getMaxPoolSize())
                .put("min_pool_size", jdbcConfig.getMinPoolSize())
                .put("user", jdbcConfig.getUsername())
                .put("password", jdbcConfig.getPassword());

        return JDBCClient.createShared(vertx, object);
    }
}
