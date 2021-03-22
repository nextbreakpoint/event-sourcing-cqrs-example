package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static rx.Single.fromCallable;

public class Authentication {
    private static final Logger logger = LoggerFactory.getLogger(Authentication.class.getName());

    public static final String NULL_USER_UUID = new UUID(0, 0).toString();

    public static final String JWT_SUBJECT = "designs";
    public static final int JWT_EXPIRES_IN_MINUTES = 60 * 24;

    public static final int COOKIE_MAX_AGE = 3600 * 24;
    public static final String COOKIE_NAME = "token";
    public static final String COOKIE_PATH = "/";

    private Authentication() {}

    public static String makeAuthorization(String accessToken) {
        return "Bearer " + accessToken;
    }

    public static String makeAnonymousAuthorization(JWTAuth jwtProvider) {
        return generateToken(jwtProvider, NULL_USER_UUID, Arrays.asList(Authority.ANONYMOUS));
    }

    public static String generateToken(JWTAuth jwtProvider, String userUuid, List<String> authorities) {
        logger.debug("Generate new JWT token for user " + userUuid);
        return jwtProvider.generateToken(makeUserObject(userUuid), makeJWTOptions(authorities));
    }

    public static JsonObject makeUserObject(String userUuid) {
        return new JsonObject().put("user", userUuid);
    }

    public static Single<User> isUserAuthorized(JWTAuth jwtProvider, RoutingContext routingContext, List<String> roles) {
        return getUser(jwtProvider, routingContext).flatMap(user -> userHasRole(roles, user));
    }

    protected static Single<User> userHasRole(List<String> roles, User user) {
        return hasRole(user, roles).map(role -> Single.just(user))
                .orElseGet(() -> Single.error(Failure.accessDenied("User doesn't have required role")));
    }

    public static Cookie createCookie(String token, String domain) {
        return Cookie.cookie(COOKIE_NAME, token)
                .setDomain(domain)
                .setPath(COOKIE_PATH)
                .setMaxAge(COOKIE_MAX_AGE);
    }

    private static JWTOptions makeJWTOptions(List<String> authorities) {
        final JWTOptions jwtOptions = new JWTOptions()
                .setExpiresInMinutes(JWT_EXPIRES_IN_MINUTES)
                .setSubject(JWT_SUBJECT);
        authorities.forEach(jwtOptions::addPermission);
        return jwtOptions;
    }

    public static Optional<String> hasRole(User user, List<String> roles) {
        return roles.stream().filter(role -> user.rxIsAuthorized(role).toBlocking().value()).findFirst();
    }

    public static Single<User> getUser(JWTAuth jwtProvider, RoutingContext routingContext) {
        return fromCallable(() -> getToken(routingContext))
                .flatMap(authorization -> makeAuthInfo(jwtProvider, authorization))
                .flatMap(jwtProvider::rxAuthenticate);
    }

    public static String getToken(RoutingContext routingContext) {
        final String authorization = routingContext.request().getHeader(Headers.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            logger.debug("Authorisation header is present");
            return authorization.substring("Bearer ".length());
        } else {
            final Cookie cookie = routingContext.getCookie("token");
            if (cookie != null && !cookie.getValue().isEmpty()) {
                logger.debug("Token cookie is present");
                return cookie.getValue();
            }
        }
        logger.debug("Authorisation header and token cookie not present");
        return null;
    }

    private static Single<JsonObject> makeAuthInfo(JWTAuth jwtProvider, String token) {
        return Single.just(makeJWT(token != null ? token : Authentication.makeAnonymousAuthorization(jwtProvider)));
    }

    private static JsonObject makeJWT(String authorization) {
        return new JsonObject().put("token", authorization);
    }
}
