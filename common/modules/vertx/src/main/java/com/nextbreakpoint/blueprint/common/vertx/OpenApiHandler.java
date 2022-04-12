package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.streams.ReadStream;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Executor;

import static io.vertx.rxjava.core.http.HttpHeaders.CONTENT_TYPE;

public class OpenApiHandler implements Handler<RoutingContext> {
    private Vertx vertx;
    private Executor executor;
    private String resourceName;

    public OpenApiHandler(Vertx vertx, Executor executor, String resourceName) {
        this.vertx = Objects.requireNonNull(vertx);
        this.executor = Objects.requireNonNull(executor);
        this.resourceName = Objects.requireNonNull(resourceName);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Single.fromCallable(this::getResourceAsStream)
                .flatMap(stream -> handle(routingContext, stream))
                .doOnError(err -> fail(routingContext, err))
                .subscribe();
    }

    private Single<Void> handle(RoutingContext routingContext, InputStream stream) {
        return Single.fromCallable(() -> createReadStream(stream))
                .flatMap(readStream -> send(routingContext, readStream))
                .doAfterTerminate(() -> closeQuietly(stream));
    }

    private void fail(RoutingContext routingContext, Throwable err) {
        routingContext.fail(Failure.requestFailed(err));
    }

    private Single<Void> send(RoutingContext routingContext, ReadStream<Buffer> readStream) {
        return routingContext.response().putHeader(CONTENT_TYPE, "text/plain").rxSend(readStream);
    }

    private InputStream getResourceAsStream() {
        return RouterBuilder.class.getClassLoader().getResourceAsStream(resourceName);
    }

    private ReadStream<Buffer> createReadStream(InputStream stream) {
        return ReadStream.newInstance(new AsyncInputStream(vertx, executor, stream));
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }
}
