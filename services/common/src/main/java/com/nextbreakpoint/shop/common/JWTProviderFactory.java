package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;

public class JWTProviderFactory {
    private JWTProviderFactory() {}

    public static JWTAuth create(Vertx vertx, JsonObject config) {
        final KeyStoreOptions options = new KeyStoreOptions()
                .setPath(config.getString("jwt_keystore_path"))
                .setType(config.getString("jwt_keystore_type"))
                .setPassword(config.getString("jwt_keystore_secret"));

        return JWTAuth.create(vertx, new JWTAuthOptions().setKeyStore(options));
    }
}
