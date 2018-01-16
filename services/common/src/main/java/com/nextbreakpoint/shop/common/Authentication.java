package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.nextbreakpoint.shop.common.Authority.ANONYMOUS;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;

public class Authentication {
    public static final String NULL_USER_UUID = new UUID(0, 0).toString();

    public static final String JWT_SUBJECT = "designs";
    public static final long JWT_EXPIRES_IN_MINUTES = 30L;

    public static final int COOKIE_MAX_AGE = 3600;
    public static final String COOKIE_NAME = "token";
    public static final String COOKIE_PATH = "/";

    private Authentication() {}

    public static String makeAuthorization(String accessToken) {
        return "Bearer " + accessToken;
    }

    public static String makeAnonymousAuthorization(JWTAuth jwtProvider) {
        return generateToken(jwtProvider, NULL_USER_UUID, Arrays.asList(ANONYMOUS));
    }

    public static String generateToken(JWTAuth jwtProvider, String userUuid, List<String> authorities) {
        return jwtProvider.generateToken(makeUserObject(userUuid), makeJWTOptions(authorities));
    }

    public static JsonObject makeUserObject(String userUuid) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.put("user", userUuid);
        return jsonObject;
    }

    public static Single<User> isUserAuthorized(JWTAuth jwtProvider, RoutingContext routingContext, List<String> roles) {
        return getUser(jwtProvider, routingContext).flatMap(user -> hasRole(user, roles)
                .map(role -> Single.just(user)).orElseGet(() -> Single.error(Failure.accessDenied())));
    }

    public static Cookie createCookie(String token, String domain) {
        final Cookie cookie = Cookie.cookie(COOKIE_NAME, token);
        cookie.setDomain(domain);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        return cookie;
    }

    private static JWTOptions makeJWTOptions(List<String> authorities) {
        final JWTOptions jwtOptions = new JWTOptions();
        jwtOptions.setExpiresInMinutes(JWT_EXPIRES_IN_MINUTES);
        jwtOptions.setSubject(JWT_SUBJECT);
        authorities.forEach(jwtOptions::addPermission);
        return jwtOptions;
    }

    public static Optional<String> hasRole(User user, List<String> roles) {
        return roles.stream().filter(role -> user.rxIsAuthorised(role).toBlocking().value()).findFirst();
    }

    public static Single<User> getUser(JWTAuth jwtProvider, RoutingContext routingContext) {
        return Single.fromCallable(() -> getToken(routingContext))
                .flatMap(authorization -> makeAuthInfo(jwtProvider, authorization))
                .flatMap(json -> jwtProvider.rxAuthenticate(json));
    }

    public static String getToken(RoutingContext routingContext) {
        final String authorization = routingContext.request().getHeader(AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        } else {
            final Cookie cookie = routingContext.getCookie("token");
            if (cookie != null && !cookie.getValue().isEmpty()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static Single<JsonObject> makeAuthInfo(JWTAuth jwtProvider, String token) {
        return Single.just(makeJWT(token != null ? token : Authentication.makeAnonymousAuthorization(jwtProvider)));
    }

    private static JsonObject makeJWT(String authorization) {
        final JsonObject json = new JsonObject();
        json.put("jwt", authorization);
        return json;
    }
}
