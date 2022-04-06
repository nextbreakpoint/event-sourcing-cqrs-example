package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;

public class Server {
    private Server() {}

    public static HttpServerOptions makeOptions(ServerConfig serverConfig) {
        final JksOptions storeOptions = new JksOptions()
                .setPath(serverConfig.getJksStorePath())
                .setPassword(serverConfig.getJksStoreSecret());

        return new HttpServerOptions().setSsl(true).setKeyStoreOptions(storeOptions);
    }
}
