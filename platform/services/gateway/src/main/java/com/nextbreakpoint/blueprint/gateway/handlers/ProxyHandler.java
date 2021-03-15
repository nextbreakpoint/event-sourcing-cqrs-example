package com.nextbreakpoint.blueprint.gateway.handlers;

import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

public class ProxyHandler implements Handler<RoutingContext> {
    private final Logger logger = LoggerFactory.getLogger(ProxyHandler.class.getName());

    private final HttpClient client;

    public ProxyHandler(HttpClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public void handle(RoutingContext context) {
        context.request()
            .bodyHandler(buffer -> handle(context, buffer))
            .exceptionHandler(err -> {
                logger.error("Error occurred while reading request", err);
                context.response().setStatusCode(500).end();
            })
            .end();
    }

    private void handle(RoutingContext context, Buffer buffer) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();
        final String traceId = context.get("request-trace-id");

        client.rxRequest(request.method(), request.uri())
                .flatMap(proxyRequest -> {
                    proxyRequest.headers().addAll(request.headers());
                    proxyRequest.putHeader("X-TRACE-ID", traceId);
                    return proxyRequest.rxSend(buffer)
                            .flatMap(proxyResponse -> {
                                response.headers().addAll(proxyResponse.headers());
                                response.setStatusCode(proxyResponse.statusCode());
                                return proxyResponse.rxBody();
                            })
                            .doOnSuccess(body -> {
                                response.write(body);
                                response.end();
                            });
                })
                .doOnError(err -> {
                    logger.error("Error occurred while processing request", err);
                    response.setStatusCode(500).end();
                })
                .subscribe();
    }
}
