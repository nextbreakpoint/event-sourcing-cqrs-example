package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.AccountsClient;
import com.nextbreakpoint.blueprint.authentication.common.GitHubClient;
import com.nextbreakpoint.blueprint.authentication.common.OAuthAdapter;
import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.authentication.common.TokenProvider;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.Cookie;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.nextbreakpoint.blueprint.common.vertx.Authentication.NULL_USER_UUID;

@Log4j2
public class GitHubSignInHandler implements Handler<RoutingContextAdapter> {
    private final OAuthAdapter oauthAdapter;
    private final AccountsClient accountsClient;
    private final GitHubClient githubClient;
    private final TokenProvider tokenProvider;

    private final String cookieDomain;

    public GitHubSignInHandler(String cookieDomain, GitHubClient githubClient, AccountsClient accountsClient, TokenProvider tokenProvider, OAuthAdapter oauthAdapter) {
        this.cookieDomain = Objects.requireNonNull(cookieDomain);
        this.accountsClient = Objects.requireNonNull(accountsClient);
        this.githubClient = Objects.requireNonNull(githubClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.oauthAdapter = Objects.requireNonNull(oauthAdapter);
    }

    @Override
    public void handle(RoutingContextAdapter routingContext) {
        if (routingContext.isUserAuthenticated()) {
            handleAuthenticatedAccess(routingContext);
        } else {
            handleUnauthenticatedAccess(routingContext);
        }
    }

    protected void handleAuthenticatedAccess(RoutingContextAdapter routingContext) {
        Single.just(newScope(routingContext))
                .flatMap(scope -> getAccessTokenOrFail(scope).map(oauthToken -> scope.toBuilder().withOauthAccessToken(oauthToken).build()))
                .flatMap(scope -> fetchUserEmail(scope).map(email -> scope.toBuilder().withUserEmail(email).build()))
                .flatMap(scope -> createPlatformAccessToken().map(accessToken -> scope.toBuilder().withJwtAccessToken(accessToken).build()))
                .flatMap(scope -> findAccounts(scope.getJwtAccessToken(), scope.getUserEmail()).map(accounts -> scope.toBuilder().withAccounts(accounts).build()))
                .flatMap(scope -> fetchOrCreateAccount(scope).map(account -> scope.toBuilder().withAccount(account).build()))
                .flatMap(scope -> createCookie(scope).map(cookie -> scope.toBuilder().withCookie(cookie).build()))
                .flatMap(this::sendRedirectResponse)
                .subscribe(scope -> {}, routingContext::handleException);
    }

    private void handleUnauthenticatedAccess(RoutingContextAdapter routingContext) {
        Single.fromCallable(() -> oauthAdapter.authorizeURL(routingContext.getRequestUri()))
                .flatMap(routingContext::sendRedirectResponse)
                .subscribe(result -> {}, routingContext::handleException);
    }

    private GitHubSignInScope newScope(RoutingContextAdapter routingContext) {
        return GitHubSignInScope.builder()
                .withRoutingContext(routingContext)
                .withRedirectTo(routingContext.getSignInRedirectUrl())
                .build();
    }

    private Single<JsonObject> fetchOrCreateAccount(GitHubSignInScope scope) {
        return scope.getAccounts().size() == 1 ? fetchAccount(scope.getJwtAccessToken(), scope.getAccounts().getString(0)) : createAccount(scope);
    }

    private Single<JsonObject> createAccount(GitHubSignInScope scope) {
        return fetchUserInfo(scope).flatMap(userInfo -> createAccount(scope.getJwtAccessToken(), scope.getUserEmail(), userInfo.getString("name")));
    }

    private Single<Cookie> createCookie(GitHubSignInScope scope) {
        final JsonObject account = scope.getAccount();
        final String uuid = account.getString("uuid");
        final String role = account.getString("role");
        if (uuid != null && role != null) {
            log.info("User role: {}", role);
            return createJwtCookie(uuid, role);
        } else {
            return Single.error(Failure.accessDenied("Missing account's uuid or role"));
        }
    }

    private Single<Void> sendRedirectResponse(GitHubSignInScope scope) {
        return scope.getRoutingContext().sendRedirectResponse(scope.getRedirectTo(), scope.getCookie());
    }

    private Single<String> createToken(String userUuid, List<String> authorities) {
        return tokenProvider.generateToken(userUuid, authorities);
    }

    private Single<String> createPlatformAccessToken() {
        return createToken(NULL_USER_UUID, List.of(Authority.PLATFORM));
    }

    private Single<Cookie> createJwtCookie(String uuid, String role) {
        return createToken(uuid, List.of(role)).map(token -> Authentication.createCookie(token, cookieDomain));
    }

    private Single<JsonArray> findAccounts(String jwtAccessToken, String email) {
        return accountsClient.findAccounts(jwtAccessToken, email);
    }

    public Single<JsonObject> createAccount(String jwtAccessToken, String email, String name) {
        return accountsClient.createAccount(jwtAccessToken, email, name);
    }

    public Single<JsonObject> fetchAccount(String jwtAccessToken, String accountId) {
        return accountsClient.fetchAccount(jwtAccessToken, accountId);
    }

    protected Single<String> fetchUserEmail(GitHubSignInScope scope) {
        return githubClient.fetchUserEmail(scope.getOauthAccessToken());
    }

    protected Single<JsonObject> fetchUserInfo(GitHubSignInScope scope) {
        return githubClient.fetchUserInfo(scope.getOauthAccessToken());
    }

    protected Single<String> getAccessTokenOrFail(GitHubSignInScope scope) {
        return Optional.ofNullable(scope.getRoutingContext().getAccessToken())
                .map(Single::just)
                .orElseGet(() -> Single.error(Failure.accessDenied("Missing OAuth access token")));
    }
}
