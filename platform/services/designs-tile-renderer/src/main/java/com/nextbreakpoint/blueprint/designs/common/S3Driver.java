package com.nextbreakpoint.blueprint.designs.common;

import rx.Single;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Objects;

public class S3Driver {
    private S3AsyncClient s3AsyncClient;
    private String bucket;

    public S3Driver(S3AsyncClient s3AsyncClient, String bucket) {
        this.s3AsyncClient = Objects.requireNonNull(s3AsyncClient);
        this.bucket = Objects.requireNonNull(bucket);
    }

    public Single<Void> putObject(String key, byte[] data) {
        return Single.from(s3AsyncClient.putObject(makePutRequest(key), AsyncRequestBody.fromBytes(data))).map(response -> null);
    }

    public Single<byte[]> getObject(String key) {
        return Single.from(s3AsyncClient.getObject(makeGetRequest(key), AsyncResponseTransformer.toBytes())).map(BytesWrapper::asByteArray);
    }

    private PutObjectRequest makePutRequest(String key) {
        return PutObjectRequest.builder().bucket(bucket).key(key).build();
    }

    private GetObjectRequest makeGetRequest(String key) {
        return GetObjectRequest.builder().bucket(bucket).key(key).build();
    }
}
