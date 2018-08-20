package com.nextbreakpoint.shop.common.vertx;

import com.nextbreakpoint.shop.common.vertx.Authentication;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Cookie;

import java.util.List;

public class TestHelper {
    private TestHelper() {}

    public static String makeAuthorization(String user, List<String> authorities) {
        Vertx vertx = Vertx.vertx();

        try {
            final JWTAuth jwtProvider = createJWTProvider(vertx);

            return "Bearer " + Authentication.generateToken(jwtProvider, user, authorities);
        } finally {
            vertx.close();
        }
    }

    public static Cookie makeCookie(String user, List<String> authorities, String domain) {
        Vertx vertx = Vertx.vertx();

        try {
            final JWTAuth jwtProvider = createJWTProvider(vertx);

            final Cookie cookie = Authentication.createCookie(Authentication.generateToken(jwtProvider, user, authorities), domain);

            return cookie;
        } finally {
            vertx.close();
        }
    }

    private static JWTAuth createJWTProvider(Vertx vertx) {
        final KeyStoreOptions options = new KeyStoreOptions()
                .setPath("../secrets/keystore-auth.jceks")
                .setType("jceks")
                .setPassword("secret");

        return JWTAuth.create(vertx, new JWTAuthOptions().setKeyStore(options));
    }
}
