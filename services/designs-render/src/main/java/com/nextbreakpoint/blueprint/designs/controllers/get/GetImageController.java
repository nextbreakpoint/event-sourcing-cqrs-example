package com.nextbreakpoint.blueprint.designs.controllers.get;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.Image;
import com.nextbreakpoint.blueprint.designs.common.Bucket;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.Objects;

@Log4j2
public class GetImageController implements Controller<GetImageRequest, GetImageResponse> {
    private final S3Driver driver;

    public GetImageController(S3Driver driver) {
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public Single<GetImageResponse> onNext(GetImageRequest request) {
        try {
            return driver.getObject(Bucket.createCacheKey(request.getChecksum()))
                    .map(bytes -> new Image(bytes, request.getChecksum()))
                    .onErrorReturn(error -> null)
                    .map(GetImageController::makeResponse);
        } catch (Exception e) {
            log.warn("Can't retrieve image", e);

            return Single.just(makeResponse(null));
        }
    }

    private static GetImageResponse makeResponse(Image image) {
        return GetImageResponse.builder()
                .withImage(image)
                .build();
    }
}
