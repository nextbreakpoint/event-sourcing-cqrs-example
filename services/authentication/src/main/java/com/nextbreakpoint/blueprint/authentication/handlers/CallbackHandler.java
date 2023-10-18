package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

import static io.vertx.core.http.HttpHeaders.*;

@Log4j2
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
        String error = ctx.request().getParam("error");
        if (error != null) {
            handleError(ctx, error);
            return;
        }

        String code = ctx.request().getParam("code");
        if (code == null) {
            ctx.fail(400, new IllegalStateException("Missing code parameter"));
            return;
        }

        String state = ctx.request().getParam("state");
        if (state == null) {
            ctx.fail(400, new IllegalStateException("Missing state parameter"));
            return;
        }

        Oauth2Credentials credentials = new Oauth2Credentials()
                .setRedirectUri(authUrl + callbackPath)
                .setFlow(OAuth2FlowType.AUTH_CODE)
                .setCode(code);

        oauthHandler.authenticate(credentials, (res) -> {
            if (res.failed()) {
                ctx.fail(res.cause());
            } else {
                ctx.setUser(res.result());
                String location = state != null ? state : "/";
                if (location.length() != 0 && location.charAt(0) == '/') {
                    ctx.reroute(location);
                    return;
                }
                ctx.response()
                        .putHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .putHeader("Pragma", "no-cache")
                        .putHeader(EXPIRES, "0")
                        .putHeader(LOCATION, location)
                        .setStatusCode(302)
                        .end("Redirecting to " + location + ".");
            }
        });
    }

    private static void handleError(RoutingContext ctx, String error) {
        short errorCode;
        switch (error) {
            case "invalid_token":
                errorCode = 401;
                break;
            case "insufficient_scope":
                errorCode = 403;
                break;
            case "invalid_request":
            default:
                errorCode = 400;
        }

        String errorDescription = ctx.request().getParam("error_description");
        if (errorDescription != null) {
            ctx.fail(errorCode, new IllegalStateException(error + ": " + errorDescription));
        } else {
            ctx.fail(errorCode, new IllegalStateException(error));
        }
    }
}
