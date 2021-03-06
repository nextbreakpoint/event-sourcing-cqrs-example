package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;

import java.util.*;

public class VertxUtils {
    private VertxUtils() {}

    public static String makeAuthorization(String user, List<String> authorities, String path) {
        Vertx vertx = Vertx.vertx();

        try {
            final JWTAuth jwtProvider = createJWTProvider(vertx, path);

            return "Bearer " + Authentication.generateToken(jwtProvider, user, authorities);
        } finally {
            vertx.close();
        }
    }

    public static Cookie makeCookie(String user, List<String> authorities, String domain, String path) {
        Vertx vertx = Vertx.vertx();

        try {
            final JWTAuth jwtProvider = createJWTProvider(vertx, path);

            final Cookie cookie = Authentication.createCookie(Authentication.generateToken(jwtProvider, user, authorities), domain);

            return cookie;
        } finally {
            vertx.close();
        }
    }

    private static JWTAuth createJWTProvider(Vertx vertx, String path) {
        final KeyStoreOptions options = new KeyStoreOptions()
                .setPath(path)
                .setType("jceks")
                .setPassword("secret");

        return JWTAuth.create(vertx, new JWTAuthOptions().setKeyStore(options));
    }
}
