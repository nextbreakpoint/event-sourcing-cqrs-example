package com.nextbreakpoint.shop.common;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;

public class ServerUtil {
    public static final String UUID_REGEXP = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";

    private ServerUtil() {}

    public static HttpServerOptions makeServerOptions(JsonObject config) {
        final String jksStorePath = config.getString("server_keystore_path");
        final String jksStoreSecret = config.getString("server_keystore_secret");
        final JksOptions storeOptions = new JksOptions().setPath(jksStorePath).setPassword(jksStoreSecret);
        return new HttpServerOptions().setSsl(true).setKeyStoreOptions(storeOptions);
    }
}
