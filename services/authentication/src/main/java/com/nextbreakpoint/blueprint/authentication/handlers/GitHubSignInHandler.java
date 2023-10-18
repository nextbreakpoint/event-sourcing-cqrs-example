package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.composer.ComposerException;
import rx.Single;

import java.util.*;

import static com.nextbreakpoint.blueprint.common.core.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.common.vertx.Authentication.NULL_USER_UUID;

@Log4j2
public class GitHubSignInHandler implements Handler<RoutingContext> {
    private final OAuth2Auth oauthHandler;
    private String oauthAuthority;
    private String callbackPath;
    private final WebClient accountsClient;
    private final WebClient githubClient;
    private final JWTAuth jwtProvider;

    private final Set<String> adminUsers;
    private final String cookieDomain;
    private final String authUrl;
    private final String webUrl;

    public GitHubSignInHandler(String cookieDomain, String webUrl, String authUrl, Set<String> adminUsers, WebClient accountsClient, WebClient githubClient, JWTAuth jwtProvider, OAuth2Auth oauthHandler, String oauthAuthority, String callbackPath) {
        this.cookieDomain = Objects.requireNonNull(cookieDomain);
        this.webUrl = Objects.requireNonNull(webUrl);
        this.authUrl = Objects.requireNonNull(authUrl);
        this.adminUsers = Objects.requireNonNull(adminUsers);
        this.accountsClient = Objects.requireNonNull(accountsClient);
        this.githubClient = Objects.requireNonNull(githubClient);
        this.jwtProvider = Objects.requireNonNull(jwtProvider);
        this.oauthHandler = Objects.requireNonNull(oauthHandler);
        this.oauthAuthority = Objects.requireNonNull(oauthAuthority);
        this.callbackPath = Objects.requireNonNull(callbackPath);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if (routingContext.user() != null) {
            handleAuthenticatedAccess(routingContext);
        } else {
            handleUnauthenticatedAccess(routingContext);
        }
    }

    private void handleUnauthenticatedAccess(RoutingContext routingContext) {
        Single.just(oauthHandler.authorizeURL(newAuthorizationURL(routingContext.request().uri())))
                .flatMap(authorizeURL -> sendRedirectResponse(routingContext, authorizeURL))
                .subscribe(result -> {}, throwable -> handleException(routingContext, throwable));
    }

    protected void handleAuthenticatedAccess(RoutingContext routingContext) {
        Single.just(newScope(routingContext))
                .flatMap(scope -> getAccessTokenOrFail(scope).map(oauthToken -> scope.toBuilder().withOauthAccessToken(oauthToken).build()))
                .flatMap(scope -> fetchUserEmail(scope).map(email -> scope.toBuilder().withUserEmail(email).build()))
                .flatMap(scope -> getJwtAccessToken().map(accessToken -> scope.toBuilder().withJwtAccessToken(accessToken).build()))
                .flatMap(scope -> findAccounts(scope).map(accounts -> scope.toBuilder().withAccounts(accounts).build()))
                .flatMap(scope -> fetchOrCreateAccount(scope).map(account -> scope.toBuilder().withAccount(account).build()))
                .flatMap(scope -> createCookie(scope).map(cookie -> scope.toBuilder().withCookie(cookie).build()))
                .flatMap(this::sendRedirectResponse)
                .subscribe(scope -> {}, throwable -> handleException(routingContext, throwable));
    }

    protected GitHubSignInScope newScope(RoutingContext routingContext) {
        return GitHubSignInScope.builder()
                .withRoutingContext(routingContext)
                .withUser(routingContext.user())
                .withRedirectTo(getRedirectTo(routingContext))
                .build();
    }

    protected Single<String> getAccessTokenOrFail(GitHubSignInScope scope) {
        return Optional.ofNullable(getAccessToken(scope.getUser()))
                .map(Single::just)
                .orElseGet(() -> Single.error(Failure.accessDenied("Missing OAuth access token")));
    }

    protected Single<JsonObject> fetchOrCreateAccount(GitHubSignInScope scope) {
        return scope.getAccounts().size() == 1 ? fetchAccount(scope) : createAccount(scope);
    }

    protected Single<JsonObject> createAccount(GitHubSignInScope scope) {
        return fetchUserInfo(scope).flatMap(userInfo -> createAccount(scope, userInfo));
    }

    protected Single<String> fetchUserEmail(GitHubSignInScope scope) {
        return githubClient.get("/user/emails")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(scope.getOauthAccessToken()))
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot retrieve user's emails", 200))
                .flatMap(response -> findPrimaryEmail(response.bodyAsJsonArray()))
                .onErrorResumeNext(this::getAuthenticationError);
    }

    protected Single<JsonObject> fetchUserInfo(GitHubSignInScope scope) {
        return githubClient.get("/user")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(scope.getOauthAccessToken()))
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot retrieve user's details", 200))
                .map(HttpResponse::bodyAsJsonObject)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    protected Single<JsonArray> findAccounts(GitHubSignInScope scope) {
        return accountsClient.get("/v1/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(scope.getJwtAccessToken()))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .addQueryParam("email", scope.getUserEmail())
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot find account", 200))
                .map(HttpResponse::bodyAsJsonArray)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    protected Single<JsonObject> createAccount(GitHubSignInScope scope, JsonObject userInfo) {
        JsonObject account = makeAccount(scope.getUserEmail(), userInfo);
        log.info("User account: " + account.encode());
        return accountsClient.post("/v1/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(scope.getJwtAccessToken()))
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSendJsonObject(account)
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot create account", 201))
                .map(HttpResponse::bodyAsJsonObject)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    protected Single<JsonObject> fetchAccount(GitHubSignInScope scope) {
        return accountsClient.get("/v1/accounts/" + scope.getAccounts().getString(0))
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(scope.getJwtAccessToken()))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot fetch account", 200))
                .map(HttpResponse::bodyAsJsonObject)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    protected Single<Cookie> createCookie(GitHubSignInScope scope) {
        final JsonObject account = scope.getAccount();
        final String uuid = account.getString("uuid");
        final String role = account.getString("role");
        if (uuid != null && role != null) {
            log.info("User role: " + role);
            final String token = Authentication.generateToken(jwtProvider, uuid, List.of(role));
            return Single.just(Authentication.createCookie(token, cookieDomain));
        } else {
            return Single.error(Failure.accessDenied("Missing account's uuid or role"));
        }
    }

    protected Single<Void> sendRedirectResponse(GitHubSignInScope scope) {
        return scope.getRoutingContext().response()
                .putHeader("Set-Cookie", scope.getCookie().encode())
                .putHeader("Location", scope.getRedirectTo())
                .setStatusCode(303)
                .rxSend();
    }

    protected String getRedirectTo(RoutingContext routingContext) {
        return webUrl + routingContext.request().path().substring("/v1/auth/signin".length());
    }

    protected void handleException(RoutingContext routingContext, Throwable throwable) {
        if (throwable instanceof Failure) {
            routingContext.fail(throwable);
        } else if (throwable instanceof ComposerException) {
            log.error("Cannot process request", throwable);
            routingContext.fail(Failure.requestFailed(throwable));
        }
    }

    protected Single<String> getJwtAccessToken() {
        return Single.fromCallable(() -> Authentication.generateToken(jwtProvider, NULL_USER_UUID, List.of(Authority.PLATFORM)));
    }

    private Single<String> findPrimaryEmail(JsonArray emails) {
        return emails.stream()
                .map(email -> (JsonObject) email)
                .filter(email -> email.getBoolean("primary"))
                .map(email -> email.getString("email"))
                .findFirst()
                .map(Single::just)
                .orElseGet(() -> Single.error(Failure.accessDenied("Cannot find primary email")));
    }

    private String getAccessToken(User user) {
        return user.principal().getString("access_token");
    }

    private Single<Void> sendRedirectResponse(RoutingContext routingContext, String location) {
        return routingContext.response()
                .putHeader("Location", location)
                .setStatusCode(302)
                .rxSend();
    }

    private JsonObject makeAccount(String userEmail, JsonObject userInfo) {
        return new JsonObject()
                .put("email", userEmail)
                .put("name", userInfo.getString("name"))
                .put("role", getAuthority(userEmail));
    }

    private String getAuthority(String userEmail) {
        return adminUsers.contains(userEmail) ? Authority.ADMIN : Authority.GUEST;
    }

    private Single<HttpResponse<Buffer>> getSuccessfulResponseOrError(HttpResponse<Buffer> response, String message, int statusCode) {
        if (response.statusCode() != statusCode) {
            return Single.error(Failure.accessDenied(message));
        } else {
            return Single.just(response);
        }
    }

    private <R> Single<R> getAuthenticationError(Throwable throwable) {
        if (throwable instanceof Failure) {
            return Single.error(throwable);
        } else {
            return Single.error(Failure.authenticationError(throwable));
        }
    }

    private OAuth2AuthorizationURL newAuthorizationURL(String uri) {
        return new OAuth2AuthorizationURL(new JsonObject()
                .put("redirect_uri", authUrl + callbackPath)
                .put("scope", oauthAuthority)
                .put("state", uri)
        );
    }
}
