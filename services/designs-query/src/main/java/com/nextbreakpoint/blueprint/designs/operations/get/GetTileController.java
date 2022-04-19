package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.designs.Store;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignRequest;
import com.nextbreakpoint.blueprint.designs.persistence.dto.LoadDesignResponse;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class GetTileController implements Controller<GetTileRequest, GetTileResponse> {
    private final Store store;
    private final S3Driver s3Driver;

    public GetTileController(Store store, S3Driver s3Driver) {
        this.store = Objects.requireNonNull(store);
        this.s3Driver = Objects.requireNonNull(s3Driver);
    }

    @Override
    public Single<GetTileResponse> onNext(GetTileRequest request) {
        return store.loadDesign(new LoadDesignRequest(request.getUuid(), request.isDraft()))
            .map(LoadDesignResponse::getDesign)
            .flatMap(result -> result.map(design -> getImage(request, design)).orElse(Single.just(new GetTileResponse(null))));
    }

    private Single<GetTileResponse> getImage(GetTileRequest request, Design document) {
        return s3Driver.getObject(getBucketKey(request, document.getChecksum()))
                .map(bytes -> new Image(bytes, document.getChecksum()))
                .doOnError(error -> log.warn("Failed to load image: " + error.getMessage()))
                .onErrorReturn(error -> null)
                .map(GetTileResponse::new);
    }

    private String getBucketKey(GetTileRequest request, String checksum) {
        return String.format("%s/%d/%04d%04d.png", checksum, request.getLevel(), request.getRow(), request.getCol());
    }
}
