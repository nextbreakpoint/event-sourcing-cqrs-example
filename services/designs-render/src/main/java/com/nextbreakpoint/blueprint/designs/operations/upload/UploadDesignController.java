package com.nextbreakpoint.blueprint.designs.operations.upload;

import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.FileManager;
import com.nextbreakpoint.nextfractal.core.common.FileManifest;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.io.File;
import java.util.List;

@Log4j2
public class UploadDesignController implements Controller<UploadDesignRequest, UploadDesignResponse> {
    @Override
    public Single<UploadDesignResponse> onNext(UploadDesignRequest request) {
        try {
            final Bundle bundle = FileManager.loadFile(new File(request.getFile())).orThrow();

            final String manifest = Json.encodeValue(new FileManifest(bundle.getSession().getPluginId()));
            final String metadata = Json.encodeValue(bundle.getSession().getMetadata());
            final String script = bundle.getSession().getScript();

            final UploadDesignResponse response = UploadDesignResponse.builder()
                    .withManifest(manifest)
                    .withMetadata(metadata)
                    .withScript(script)
                    .withErrors(List.of())
                    .build();

            return Single.just(response);
        } catch (Exception e) {
            log.warn("Can't load data", e);

            final UploadDesignResponse response = UploadDesignResponse.builder()
                    .withErrors(List.of(e.getMessage()))
                    .build();

            return Single.just(response);
        }
    }
}
