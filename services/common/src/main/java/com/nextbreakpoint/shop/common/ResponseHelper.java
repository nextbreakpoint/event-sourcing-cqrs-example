package com.nextbreakpoint.shop.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;
import java.util.function.Function;

public class ResponseHelper {
    private static final Logger logger = LoggerFactory.getLogger(ResponseHelper.class.getName());

    private ResponseHelper() {}

    public static void sendFailure(RoutingContext routingContext) {
        final Optional<Throwable> throwable = Optional.ofNullable(routingContext.failure());

        final String message = throwable.map(Throwable::getMessage)
                .orElse("Error " + routingContext.statusCode());

        final int statusCode = throwable.filter(x -> x instanceof Failure)
                .map(x -> ((Failure) x).getStatusCode())
                .orElseGet(() -> routingContext.statusCode() > 0 ? routingContext.statusCode() : 500);

        logger.warn(message);

        routingContext.response()
                .putHeader(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .setStatusCode(statusCode)
                .end(createErrorResponseObject(message).encode());
    }

    public static void redirectToError(RoutingContext routingContext, Function<Integer, String> getErrorRedirectURL) {
        final Optional<Throwable> throwable = Optional.ofNullable(routingContext.failure());

        final String message = throwable.map(Throwable::getMessage)
                .orElse("Error " + routingContext.statusCode());

        final int statusCode = throwable.filter(x -> x instanceof Failure)
                .map(x -> ((Failure) x).getStatusCode())
                .orElseGet(() -> routingContext.statusCode() > 0 ? routingContext.statusCode() : 500);

        logger.warn(message);

        routingContext.response()
                .putHeader("Location", getErrorRedirectURL.apply(statusCode))
                .setStatusCode(303)
                .end();
    }

    private static JsonObject createErrorResponseObject(String error) {
        return new JsonObject().put("error", error);
    }

    public static void sendNoContent(RoutingContext routingContext) {
        routingContext.response().setStatusCode(204).end();
    }
}
