package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.ContentType;
import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.function.Function;

@Log4j2
public class ResponseHelper {
    private ResponseHelper() {}

    public static void sendFailure(RoutingContext routingContext) {
        final Optional<Throwable> throwable = Optional.ofNullable(routingContext.failure());

        final String message = throwable.map(Throwable::getMessage)
                .orElse("Error " + routingContext.statusCode() + " (Request URL: " + routingContext.getDelegate().request().absoluteURI() + ")");

        final int statusCode = throwable.filter(x -> x instanceof Failure)
                .map(x -> ((Failure) x).getStatusCode())
                .orElseGet(() -> routingContext.statusCode() > 0 ? routingContext.statusCode() : 500);

        if (throwable.isPresent()) {
            log.warn(message, throwable.get());
        } else {
            log.warn(message);
        }

        routingContext.response()
                .putHeader(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .setStatusCode(statusCode)
                .end(createErrorResponseObject(message).encode());
    }

    public static void redirectToError(RoutingContext routingContext, Function<Integer, String> getErrorRedirectURL) {
        final Optional<Throwable> throwable = Optional.ofNullable(routingContext.failure());

        final String message = throwable.map(Throwable::getMessage)
                .orElse("Error " + routingContext.statusCode() + " (Request URL: " + routingContext.getDelegate().request().absoluteURI() + ")");

        final int statusCode = throwable.filter(x -> x instanceof Failure)
                .map(x -> ((Failure) x).getStatusCode())
                .orElseGet(() -> routingContext.statusCode() > 0 ? routingContext.statusCode() : 500);

        if (throwable.isPresent()) {
            log.warn(message, throwable.get());
        } else {
            log.warn(message);
        }

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
