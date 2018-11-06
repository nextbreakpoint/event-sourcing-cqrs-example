package com.nextbreakpoint.shop.router.handlers;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
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
        final HttpServerRequest frontRequest = context.request();

        final HttpServerResponse frontResponse = context.response();

        final HttpClientRequest proxyRequest = client.request(frontRequest.method(), frontRequest.uri(), proxyResponse -> {
            frontResponse.headers().addAll(proxyResponse.headers());

            frontResponse.setStatusCode(proxyResponse.statusCode());

            proxyResponse.exceptionHandler(error -> {
                        logger.error("Failed while processing response", error);
                        context.fail(500);
                    })
                    .handler(buffer -> frontResponse.write(buffer))
                    .endHandler(c -> frontResponse.end());
        }).exceptionHandler(error -> {
            logger.error("Failed while processing request", error);
            context.fail(500);
        });

        proxyRequest.headers().addAll(frontRequest.headers());

        proxyRequest.putHeader("X-TRACE-ID", context.get("request-trace-id"));

        frontRequest.exceptionHandler(error -> {
                    logger.error("Failed while producing request", error);
                    context.fail(500);
                })
                .handler(buffer -> proxyRequest.write(buffer))
                .endHandler(x -> proxyRequest.end());
    }
}
