package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.authentication.common.AccountsClient;
import com.nextbreakpoint.blueprint.authentication.common.GitHubClient;
import com.nextbreakpoint.blueprint.authentication.common.OAuthAdapter;
import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.authentication.common.RoutingContextHandlerAdapter;
import com.nextbreakpoint.blueprint.authentication.common.TokenProvider;
import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInHandler;
import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInScope;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

public class VerticleStub extends Verticle {
    public static final String DEFAULT_EMAIL = "test@localhost";

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", VerticleStub.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/default.json" });
    }

    @Override
    protected Handler<RoutingContext> createSignInHandler(String webUrl, String cookieDomain, GitHubClient githubClient, AccountsClient accountsClient, TokenProvider tokenProvider, OAuthAdapter oauthAdapter) {
        return new RoutingContextHandlerAdapter(webUrl, new GitHubSignInHandler(cookieDomain, githubClient, accountsClient, tokenProvider, oauthAdapter) {
            @Override
            public void handle(RoutingContextAdapter routingContext) {
                handleAuthenticatedAccess(routingContext);
            }

            @Override
            protected Single<String> getAccessTokenOrFail(GitHubSignInScope scope) {
                return Single.just(null);
            }

            @Override
            protected Single<String> fetchUserEmail(GitHubSignInScope scope) {
                final String email = scope.getRoutingContext().getRequestParam("email");
                return Single.just(email != null ? email : DEFAULT_EMAIL);
            }

            @Override
            protected Single<JsonObject> fetchUserInfo(GitHubSignInScope scope) {
                return Single.just(new JsonObject().put("name", "Micky Mouse"));
            }
        });
    }
}
