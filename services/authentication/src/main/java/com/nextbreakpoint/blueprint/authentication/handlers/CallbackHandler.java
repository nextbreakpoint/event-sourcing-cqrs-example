package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;
import static io.vertx.core.http.HttpHeaders.EXPIRES;
import static io.vertx.core.http.HttpHeaders.LOCATION;

public class CallbackHandler implements Handler<RoutingContext> {
    private final String authUrl;
    private final OAuth2Auth oauthHandler;
    private final String callbackPath;

    public CallbackHandler(String authUrl, OAuth2Auth oauthHandler, String callbackPath) {
        this.authUrl = Objects.requireNonNull(authUrl);
        this.oauthHandler = Objects.requireNonNull(oauthHandler);
        this.callbackPath = Objects.requireNonNull(callbackPath);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            handleCallback(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void handleCallback(RoutingContext ctx) {
        final String error = ctx.request().getParam("error");
        if (error != null) {
            handleError(ctx, error);
            return;
        }

        final String code = ctx.request().getParam("code");
        if (code == null) {
            ctx.fail(400, new IllegalStateException("Missing code parameter"));
            return;
        }

        final String state = ctx.request().getParam("state");
        if (state == null) {
            ctx.fail(400, new IllegalStateException("Missing state parameter"));
            return;
        }

        final Oauth2Credentials credentials = new Oauth2Credentials()
                .setRedirectUri(authUrl + callbackPath)
                .setFlow(OAuth2FlowType.AUTH_CODE)
                .setCode(code);

        oauthHandler.authenticate(credentials, (res) -> {
            if (res.failed()) {
                ctx.fail(res.cause());
            } else {
                ctx.setUser(res.result());
                if (state.length() != 0 && state.charAt(0) == '/') {
                    ctx.reroute(state);
                } else {
                    ctx.response()
                        .putHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .putHeader("Pragma", "no-cache")
                        .putHeader(EXPIRES, "0")
                        .putHeader(LOCATION, state)
                        .setStatusCode(302)
                        .end("Redirecting to " + state + ".");
                }
            }
        });
    }

    private static void handleError(RoutingContext ctx, String error) {
        short errorCode = switch (error) {
            case "invalid_token" -> 401;
            case "insufficient_scope" -> 403;
            case "invalid_request" -> 400;
            default -> 400;
        };

        final String errorDescription = ctx.request().getParam("error_description");
        if (errorDescription != null) {
            ctx.fail(errorCode, new IllegalStateException(error + ": " + errorDescription));
        } else {
            ctx.fail(errorCode, new IllegalStateException(error));
        }
    }
}
