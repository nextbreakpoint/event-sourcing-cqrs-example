package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.LoadDesignResponse;
import rx.Single;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Objects;
import java.util.Optional;

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
            .map(LoadDesignResponse::getDesign)
            .flatMap(result -> result.map(design -> getImage(request, design)).orElse(Single.just(new GetTileResponse(null))));
    }

    private Single<GetTileResponse> getImage(GetTileRequest request, Design document) {
        return s3Driver.getObject(getBucketKey(request, document.getChecksum()))
                .onErrorReturn(null)
                .map(data -> getImage(document, data));
    }

    private GetTileResponse getImage(Design document, byte[] data) {
        return Optional.ofNullable(data)
                .map(bytes -> new Image(bytes, document.getChecksum()))
                .map(GetTileResponse::new)
                .orElseGet(() -> new GetTileResponse(null));
    }

    private String getBucketKey(GetTileRequest request, String checksum) {
        return String.format("%s/%d/%04d%04d.png", checksum, request.getLevel(), request.getRow(), request.getCol());
    }
}
