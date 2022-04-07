package com.nextbreakpoint.blueprint.designs.common;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import rx.Single;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.Objects;

public class S3Driver {
    private final S3AsyncClient s3AsyncClient;
    private final String bucket;
    private final Tracer tracer;

    public S3Driver(S3AsyncClient s3AsyncClient, String bucket) {
        this.s3AsyncClient = Objects.requireNonNull(s3AsyncClient);
        this.bucket = Objects.requireNonNull(bucket);

        tracer = GlobalOpenTelemetry.getTracer("com.nextbreakpoint.blueprint");
    }

    public Single<byte[]> getObject(String key) {
        return Single.fromCallable(() -> getContent(key)).map(BytesWrapper::asByteArray);
    }

    private ResponseBytes<GetObjectResponse> getContent(String key) {
        final Span objectSpan = tracer.spanBuilder("Get object " + key).startSpan();

        try (Scope scope = objectSpan.makeCurrent()) {
            final Span span = Span.current();

            span.setAttribute("object.key", key);

            return s3AsyncClient.getObject(makeGetRequest(key), AsyncResponseTransformer.toBytes()).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            objectSpan.end();
        }
    }

    private GetObjectRequest makeGetRequest(String key) {
        return GetObjectRequest.builder().bucket(bucket).key(key).build();
    }
}
