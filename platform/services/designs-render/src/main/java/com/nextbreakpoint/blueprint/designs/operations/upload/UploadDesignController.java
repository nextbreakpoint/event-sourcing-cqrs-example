package com.nextbreakpoint.blueprint.designs.operations.upload;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.FileManager;
import com.nextbreakpoint.nextfractal.core.common.FileManifest;
import rx.Single;

import java.io.File;
import java.util.List;

public class UploadDesignController implements Controller<UploadDesignRequest, UploadDesignResponse> {
    public UploadDesignController() {}

    @Override
    public Single<UploadDesignResponse> onNext(UploadDesignRequest request) {
        return Single.just(request).flatMap(this::onRequest);
    }

    private Single<UploadDesignResponse> onRequest(UploadDesignRequest request) {
        try {
            final Bundle bundle = FileManager.loadFile(new File(request.getFile())).orThrow();
            final String manifest = Json.encodeValue(new FileManifest(bundle.getSession().getPluginId()));
            final String metadata = Json.encodeValue(bundle.getSession().getMetadata());
            final String script = bundle.getSession().getScript();
            return Single.just(new UploadDesignResponse(manifest, metadata, script, List.of()));
        } catch (Exception e) {
            return Single.just(new UploadDesignResponse(null, null, null, List.of(e.getMessage())));
        }
    }
}
