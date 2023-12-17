package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.OAuthAdapter;
import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;

import java.util.Objects;

public class CallbackHandler implements Handler<RoutingContextAdapter> {
    private final OAuthAdapter oauthAdapter;

    public CallbackHandler(OAuthAdapter oauthAdapter) {
        this.oauthAdapter = Objects.requireNonNull(oauthAdapter);
    }

    @Override
    public void handle(RoutingContextAdapter routingContext) {
        try {
            handleCallback(routingContext);
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void handleCallback(RoutingContextAdapter routingContext) {
        final String error = routingContext.getRequestParam("error");
        if (error != null) {
            handleError(routingContext);
            return;
        }

        final String code = routingContext.getRequestParam("code");
        if (code == null) {
            routingContext.fail(400, new IllegalStateException("Missing code parameter"));
            return;
        }

        final String state = routingContext.getRequestParam("state");
        if (state == null) {
            routingContext.fail(400, new IllegalStateException("Missing state parameter"));
            return;
        }

        oauthAdapter.authenticate(code, res -> {
            if (res.failed()) {
                routingContext.fail(500, res.cause());
            } else {
                routingContext.setUser(res.result());
                if (state.length() != 0 && state.charAt(0) == '/') {
                    routingContext.reroute(state);
                } else {
                    routingContext.sendRedirectResponse(state);
                }
            }
        });
    }

    private static void handleError(RoutingContextAdapter routingContext) {
        final String error = routingContext.getRequestParam("error");
        final String errorDescription = routingContext.getRequestParam("error_description");
        if (errorDescription != null) {
            routingContext.fail(getErrorCode(error), new IllegalStateException(error + ": " + errorDescription));
        } else {
            routingContext.fail(getErrorCode(error), new IllegalStateException(error));
        }
    }

    private static short getErrorCode(String error) {
        return switch (error) {
            case "invalid_token" -> 401;
            case "insufficient_scope" -> 403;
            case "invalid_request" -> 400;
            default -> 400;
        };
    }
}
