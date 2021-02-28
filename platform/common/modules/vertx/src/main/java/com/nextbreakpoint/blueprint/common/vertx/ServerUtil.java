package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Environment;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;

public class ServerUtil {
    public static final String UUID_REGEXP = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";

    private ServerUtil() {}

    public static HttpServerOptions makeServerOptions(Environment environment, JsonObject config) {
        final String jksStorePath = environment.resolve(config.getString("server_keystore_path"));
        final String jksStoreSecret = environment.resolve(config.getString("server_keystore_secret"));
        final JksOptions storeOptions = new JksOptions().setPath(jksStorePath).setPassword(jksStoreSecret);
        return new HttpServerOptions().setSsl(true).setKeyStoreOptions(storeOptions);
    }
}
