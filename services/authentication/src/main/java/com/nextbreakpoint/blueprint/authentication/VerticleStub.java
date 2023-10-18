package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInHandler;
import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInScope;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

import java.util.Set;

public class VerticleStub extends Verticle {
    public static final String DEFAULT_EMAIL = "test@localhost";

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", VerticleStub.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/default.json" });
    }

    @Override
    protected Handler<RoutingContext> createSignInHandler(String cookieDomain, String webUrl, String authUrl, Set<String> adminUsers, WebClient accountsClient, WebClient githubClient, JWTAuth jwtProvider, OAuth2Auth oauthHandler, String oauthAuthority, String callbackPath) {
        return new GitHubSignInHandler(cookieDomain, webUrl, authUrl, adminUsers, accountsClient, githubClient, jwtProvider, oauthHandler, oauthAuthority, callbackPath) {
            @Override
            public void handle(RoutingContext routingContext) {
                handleAuthenticatedAccess(routingContext);
            }

            @Override
            protected Single<String> getAccessTokenOrFail(GitHubSignInScope scope) {
                return Single.just(null);
            }

            @Override
            protected Single<String> fetchUserEmail(GitHubSignInScope scope) {
                final String email = scope.getRoutingContext().request().getParam("email");
                return Single.just(email != null ? email : DEFAULT_EMAIL);
            }

            @Override
            protected Single<JsonObject> fetchUserInfo(GitHubSignInScope scope) {
                return Single.just(new JsonObject().put("name", "Micky Mouse"));
            }
        };
    }
}
