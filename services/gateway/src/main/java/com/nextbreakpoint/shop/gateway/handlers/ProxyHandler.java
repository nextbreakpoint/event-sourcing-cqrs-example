package com.nextbreakpoint.shop.gateway.handlers;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;
import java.util.UUID;

public class ProxyHandler implements Handler<RoutingContext> {
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
                error.printStackTrace();
            }).handler(buffer -> {
                frontResponse.write(buffer);
            }).endHandler(c -> {
                frontResponse.end();
            });
        }).exceptionHandler(error -> {
            error.printStackTrace();
        });

        proxyRequest.headers().addAll(frontRequest.headers());

        proxyRequest.putHeader("X-TRACE-ID", UUID.randomUUID().toString());

        frontRequest.exceptionHandler(error -> {
            error.printStackTrace();
        }).endHandler(x -> {
            proxyRequest.end();
        }).handler(buffer -> {
            proxyRequest.write(buffer);
        });
    }
}
