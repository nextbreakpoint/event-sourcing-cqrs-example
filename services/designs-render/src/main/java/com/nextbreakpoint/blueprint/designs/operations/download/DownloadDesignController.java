package com.nextbreakpoint.blueprint.designs.operations.download;

import com.nextbreakpoint.Try;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.designs.common.BundleUtils;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import rx.Single;

public class DownloadDesignController implements Controller<DownloadDesignRequest, DownloadDesignResponse> {
    @Override
    public Single<DownloadDesignResponse> onNext(DownloadDesignRequest request) {
        try {
            final Try<Bundle, Exception> bundle = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript());

            final Try<byte[], Exception> result = BundleUtils.writeBundle(bundle.orThrow());

            final DownloadDesignResponse response = DownloadDesignResponse.builder()
                    .withBytes(result.orElse(null))
                    .build();

            return Single.just(response);
        } catch (Exception e) {
            final DownloadDesignResponse response = DownloadDesignResponse.builder()
                    .withBytes(null)
                    .build();

            return Single.just(response);
        }
    }
}
