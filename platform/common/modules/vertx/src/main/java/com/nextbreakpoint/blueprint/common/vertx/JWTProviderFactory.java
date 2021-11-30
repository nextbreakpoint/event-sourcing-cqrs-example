package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
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

        final JWTOptions jwtOptions = new JWTOptions().setLeeway(5);

        final JWTAuthOptions authOptions = new JWTAuthOptions()
                .setKeyStore(options)
                .setJWTOptions(jwtOptions);

        return JWTAuth.create(vertx, authOptions);
    }
}
