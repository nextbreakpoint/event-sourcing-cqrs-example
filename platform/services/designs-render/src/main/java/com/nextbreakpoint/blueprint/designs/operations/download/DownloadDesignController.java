package com.nextbreakpoint.blueprint.designs.operations.download;

import com.nextbreakpoint.Try;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
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
            Bundle bundle = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript());

            Try<byte[], Exception> result = BundleUtils.writeBundle(bundle);

            return Single.just(new DownloadDesignResponse(result.orElse(null)));
        } catch (Exception e) {
            return Single.just(new DownloadDesignResponse(null));
        }
    }
}
