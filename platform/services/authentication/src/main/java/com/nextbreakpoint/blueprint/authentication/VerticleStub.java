package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInHandler;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.net.MalformedURLException;

public class VerticleStub extends Verticle {
    public static final String DEFAULT_EMAIL = "test@localhost";

    public static void main(String[] args) {
        Launcher.main(new String[] { "run", VerticleStub.class.getCanonicalName(), "-conf", args.length > 0 ? args[0] : "config/default.json" });
    }

    @Override
    protected GitHubSignInHandler createSignInHandler(Environment environment, JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSignInHandler(environment, vertx, config, router) {
            @Override
            public void handle(RoutingContext routingContext) {
                try {
                    final String email = routingContext.request().getParam("email");
                    findAccount(routingContext, getRedirectTo(routingContext), null, email != null ? email : DEFAULT_EMAIL);
                } catch (Exception e) {
                    routingContext.fail(Failure.requestFailed(e));
                }
            }

            @Override
            protected void fetchUserInfo(RoutingContext routingContext, String redirectTo, String accessToken, String oauthAccessToken, String userEmail) {
                final JsonObject userInfo = new JsonObject().put("name", "Micky Mouse");
                createAccount(routingContext, redirectTo, accessToken, userEmail, userInfo);
            }
        };
    }
}
