package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.common.core.Image;
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
            .flatMap(result -> result.map(document -> getImage(request, document)).orElse(Single.just(new GetTileResponse(null))));
    }

    private Single<GetTileResponse> getImage(GetTileRequest request, DesignDocument document) {
        return s3Driver.getObject(getBucketKey(request, document.getChecksum()))
                .onErrorReturn(this::handleError)
                .map(data -> new GetTileResponse(new Image(data, document.getChecksum())));
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
