package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;

public class JWTProviderFactory {
    private JWTProviderFactory() {}

    public static JWTAuth create(Vertx vertx, JWTProviderConfig jwtConfig) {
        final KeyStoreOptions options = new KeyStoreOptions()
                .setType(jwtConfig.getKeyStoreType())
                .setPath(jwtConfig.getKeyStorePath())
                .setPassword(jwtConfig.getKeyStoreSecret());

        final JWTOptions jwtOptions = new JWTOptions().setLeeway(5);

        final JWTAuthOptions authOptions = new JWTAuthOptions()
                .setKeyStore(options)
                .setJWTOptions(jwtOptions);

        return JWTAuth.create(vertx, authOptions);
    }
}
