package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.operations.load.LoadDesignResponse;
import rx.Single;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Objects;

public class GetTileController implements Controller<GetTileRequest, GetTileResponse> {
    private Store store;
    private final S3Driver s3Driver;

    public GetTileController(Store store, S3Driver s3Driver) {
        this.store = Objects.requireNonNull(store);
        this.s3Driver = Objects.requireNonNull(s3Driver);
    }

    @Override
    public Single<GetTileResponse> onNext(GetTileRequest request) {
        return store.loadDesign(new LoadDesignRequest(request.getUuid()))
            .map(LoadDesignResponse::getDesignDocument)
            .map(result -> result.map(document -> getBucketKey(request, document.getChecksum())))
            .flatMap(result -> result.map(key -> s3Driver.getObject(key).onErrorReturn(this::handleError)).orElse(Single.just(null)))
            .map(GetTileResponse::new);
    }

    private String getBucketKey(GetTileRequest request, String checksum) {
        return String.format("%s/%d/%04d%04d.png", checksum, request.getLevel(), request.getRow(), request.getCol());
    }

    private byte[] handleError(Throwable err) {
        if (err.getCause() != null && err.getCause() instanceof NoSuchKeyException) {
            return null;
        }
        throw new RuntimeException(err);
    }
}
