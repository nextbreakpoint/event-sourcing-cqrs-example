package com.nextbreakpoint.shop.auth;

import com.nextbreakpoint.shop.common.Failure;
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
    protected GitHubSigninHandler createSigninHandler(JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSigninHandler(vertx, config, router) {
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
                final JsonObject userInfo = new JsonObject();
                userInfo.put("name", "Micky Mouse");
                createAccount(routingContext, redirectTo, accessToken, userEmail, userInfo);
            }
        };
    }
}
