package com.nextbreakpoint.blueprint.designs;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.List;

public class TestS3 {
    private TestS3() {}

    @NotNull
    public static S3Client createS3Client(URI endpoint) {
        return S3Client.builder()
                .region(Region.EU_WEST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("admin", "password")))
                .endpointOverride(endpoint)
                .build();
    }

    public static void deleteObjects(S3Client s3Client, String bucket, List<S3Object> objects) {
        objects.forEach(object -> deleteObject(s3Client, bucket, object.key()));
    }

    public static DeleteObjectResponse deleteObject(S3Client s3Client, String bucket, String key) {
        return s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public static ResponseBytes<GetObjectResponse> getObject(S3Client s3Client, String bucket, String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public static PutObjectResponse putObject(S3Client s3Client, String bucket, String key, byte[] data) {
        return s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromBytes(data));
    }

    public static void deleteContent(S3Client s3Client, String bucket) {
        s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).build())
                .stream()
                .forEach(response -> TestS3.deleteObjects(s3Client, bucket, response.contents()));
    }

    public static void deleteBucket(S3Client s3Client, String bucket) {
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
    }

    public static void createBucket(S3Client s3Client, String bucket) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }
}
