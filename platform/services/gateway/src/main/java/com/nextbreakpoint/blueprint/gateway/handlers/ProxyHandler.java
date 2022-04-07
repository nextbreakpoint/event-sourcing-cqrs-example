package com.nextbreakpoint.blueprint.gateway.handlers;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Log4j2
public class ProxyHandler implements Handler<RoutingContext> {
    private final HttpClient client;

    public ProxyHandler(HttpClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public void handle(RoutingContext context) {
        context.request()
            .bodyHandler(buffer -> handle(context, buffer))
            .exceptionHandler(err -> {
                log.error("Error occurred while reading request", err);
                context.response().setStatusCode(500).end();
            })
            .end();
    }

    private void handle(RoutingContext context, Buffer buffer) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();

        client.rxRequest(request.method(), request.uri())
                .flatMap(proxyRequest -> {
                    proxyRequest.headers().addAll(request.headers());
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
                    log.error("Error occurred while processing request", err);
                    response.setStatusCode(500).end();
                })
                .subscribe();
    }
}
