package com.nextbreakpoint.blueprint.designs.operations.download;

import com.nextbreakpoint.Try;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.designs.common.BundleUtils;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import rx.Single;

public class DownloadDesignController implements Controller<DownloadDesignRequest, DownloadDesignResponse> {
    public DownloadDesignController() {}

    @Override
    public Single<DownloadDesignResponse> onNext(DownloadDesignRequest request) {
        return Single.just(request).flatMap(this::onRequest);
    }

    private Single<DownloadDesignResponse> onRequest(DownloadDesignRequest request) {
        try {
            final Bundle bundle = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript());

            final Try<byte[], Exception> result = BundleUtils.writeBundle(bundle);

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
