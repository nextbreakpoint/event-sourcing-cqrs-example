package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;

public class JWTProviderFactory {
    private JWTProviderFactory() {}

    public static JWTAuth create(Vertx vertx, JsonObject config) {
        final JsonObject keyStoreObject = new JsonObject()
                .put("path", config.getString("jwt_keystore_path"))
                .put("type", config.getString("jwt_keystore_type"))
                .put("password", config.getString("jwt_keystore_secret"));

        return JWTAuth.create(vertx, new JsonObject().put("keyStore", keyStoreObject));
    }
}
