package com.nextbreakpoint.blueprint.designs.operations.parse;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.vertx.Controller;
import com.nextbreakpoint.nextfractal.core.common.*;
import rx.Single;

import java.io.File;
import java.util.List;

public class ParseDesignController implements Controller<ParseDesignRequest, ParseDesignResponse> {
    public ParseDesignController() {}

    @Override
    public Single<ParseDesignResponse> onNext(ParseDesignRequest request) {
        return Single.just(request).flatMap(this::onRequest);
    }

    private Single<ParseDesignResponse> onRequest(ParseDesignRequest request) {
        try {
            final Bundle bundle = FileManager.loadFile(new File(request.getFile())).orThrow();
            final String manifest = Json.encodeValue(new FileManifest(bundle.getSession().getPluginId()));
            final String metadata = Json.encodeValue(bundle.getSession().getMetadata());
            final String script = bundle.getSession().getScript();
            return Single.just(new ParseDesignResponse(manifest, metadata, script, List.of()));
        } catch (Exception e) {
            return Single.just(new ParseDesignResponse(null, null, null, List.of(e.getMessage())));
        }
    }
}
