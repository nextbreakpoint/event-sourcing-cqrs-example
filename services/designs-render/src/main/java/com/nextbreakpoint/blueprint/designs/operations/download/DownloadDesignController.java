package com.nextbreakpoint.blueprint.designs.operations.download;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.designs.common.BundleUtils;
import lombok.extern.log4j.Log4j2;
import rx.Single;

@Log4j2
public class DownloadDesignController implements Controller<DownloadDesignRequest, DownloadDesignResponse> {
    @Override
    public Single<DownloadDesignResponse> onNext(DownloadDesignRequest request) {
        try {
            final byte[] result = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript())
                    .flatMap(BundleUtils::writeBundle)
                    .orThrow();

            return Single.just(createResponse(result));
        } catch (Exception e) {
            log.warn("Can't write data", e);

            return Single.just(createResponse(null));
        }
    }

    private static DownloadDesignResponse createResponse(byte[] result) {
        return DownloadDesignResponse.builder()
                .withBytes(result)
                .build();
    }
}
