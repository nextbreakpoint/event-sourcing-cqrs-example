package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import rx.Single;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.util.Objects;

public class GetTileController implements Controller<GetTileRequest, GetTileResponse> {
    private final S3AsyncClient s3AsyncClient;
    private final String s3Bucket;

    public GetTileController(S3AsyncClient s3AsyncClient, String s3Bucket) {
        this.s3AsyncClient = Objects.requireNonNull(s3AsyncClient);
        this.s3Bucket = Objects.requireNonNull(s3Bucket);
    }

    @Override
    public Single<GetTileResponse> onNext(GetTileRequest request) {
        //TODO
        return Single.just(new GetTileResponse(null));
    }
}
