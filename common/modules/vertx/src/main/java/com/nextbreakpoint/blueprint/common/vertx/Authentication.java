package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static rx.Single.fromCallable;

@Log4j2
public class Authentication {
    public static final String NULL_USER_UUID = new UUID(0, 0).toString();

    public static final String JWT_SUBJECT = "designs";
    public static final int JWT_EXPIRES_IN_MINUTES = 3600 * 24 + 600;

    public static final int COOKIE_MAX_AGE = 3600 * 24;
    public static final String COOKIE_NAME = "token";
    public static final String COOKIE_PATH = "/";

    private Authentication() {}

    public static String makeAuthorization(String accessToken) {
        return "Bearer " + accessToken;
    }

    public static String makeAnonymousAuthorization(JWTAuth jwtProvider) {
        return generateToken(jwtProvider, NULL_USER_UUID, List.of(Authority.ANONYMOUS));
    }

    public static String generateToken(JWTAuth jwtProvider, String userUuid, List<String> authorities) {
        log.debug("Generate new JWT token for user {}", userUuid);
        return jwtProvider.generateToken(makeUserObject(userUuid), makeJWTOptions(authorities));
    }

    public static Single<User> isUserAuthorized(JWTAuth jwtProvider, RoutingContext routingContext, List<String> roles) {
        return getUser(jwtProvider, routingContext).flatMap(user -> userHasRole(roles, user));
    }

    public static Cookie createCookie(String token, String domain) {
        return Cookie.cookie(COOKIE_NAME, token)
                .setDomain(domain)
                .setPath(COOKIE_PATH)
                .setMaxAge(COOKIE_MAX_AGE);
    }

    private static Single<User> userHasRole(List<String> roles, User user) {
        return hasRole(user, roles).map(role -> Single.just(user))
                .orElseGet(() -> Single.error(Failure.accessDenied("User doesn't have required role")));
    }

    private static JsonObject makeUserObject(String userUuid) {
        return new JsonObject().put("user", userUuid);
    }

    private static Optional<String> hasRole(User user, List<String> roles) {
        return roles.stream().filter(role -> user.rxIsAuthorized(role).toBlocking().value()).findFirst();
    }

    private static Single<User> getUser(JWTAuth jwtProvider, RoutingContext routingContext) {
        return fromCallable(() -> getToken(routingContext))
                .flatMap(authorization -> makeAuthInfo(jwtProvider, authorization))
                .flatMap(jwtProvider::rxAuthenticate);
    }

    private static String getToken(RoutingContext routingContext) {
        final String authorization = routingContext.request().getHeader(Headers.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            log.debug("Authorisation header is present");
            return authorization.substring("Bearer ".length());
        } else {
            final Cookie cookie = routingContext.getCookie("token");
            if (cookie != null && !cookie.getValue().isEmpty()) {
                log.debug("Token cookie is present");
                return cookie.getValue();
            }
        }
        log.debug("Authorisation header and token cookie not present");
        return null;
    }

    private static JWTOptions makeJWTOptions(List<String> authorities) {
        final JWTOptions jwtOptions = new JWTOptions()
                .setExpiresInMinutes(JWT_EXPIRES_IN_MINUTES)
                .setSubject(JWT_SUBJECT);
        authorities.forEach(jwtOptions::addPermission);
        return jwtOptions;
    }

    private static Single<JsonObject> makeAuthInfo(JWTAuth jwtProvider, String token) {
        return Single.just(makeJWT(token != null ? token : Authentication.makeAnonymousAuthorization(jwtProvider)));
    }

    private static JsonObject makeJWT(String authorization) {
        return new JsonObject().put("token", authorization);
    }
}
