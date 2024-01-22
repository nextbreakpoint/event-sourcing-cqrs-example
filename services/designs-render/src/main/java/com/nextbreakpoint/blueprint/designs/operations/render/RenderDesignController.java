package com.nextbreakpoint.blueprint.designs.operations.render;

import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.Controller;
import com.nextbreakpoint.blueprint.designs.common.BundleUtils;
import com.nextbreakpoint.blueprint.designs.common.Render;
import com.nextbreakpoint.blueprint.designs.common.S3Driver;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.TileGenerator;
import com.nextbreakpoint.nextfractal.core.common.TileRequest;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import rx.Single;

import java.util.List;
import java.util.Objects;

@Log4j2
public class RenderDesignController implements Controller<RenderDesignRequest, RenderDesignResponse> {
    private final S3Driver driver;

    public RenderDesignController(S3Driver driver) {
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public Single<RenderDesignResponse> onNext(RenderDesignRequest request) {
        try {
            final Bundle bundle = BundleUtils.createBundle(request.getManifest(), request.getMetadata(), request.getScript()).orThrow();

            final TileRequest tileRequest = TileGenerator.createTileRequest(512, 1, 1, 0, 0, bundle);

            final String checksum = Checksum.of(encodeData(request));

            return driver.getObject(Render.getCacheKey(checksum))
                    .map(image -> null)
                    .onErrorResumeNext(error -> saveImage(tileRequest, checksum))
                    .map(result -> makeResponse(checksum, List.of()))
                    .onErrorReturn(error -> makeResponse(checksum, List.of("Can't render image: " + error.getMessage())));
        } catch (Exception e) {
            log.warn("Can't render image", e);

            return Single.just(makeResponse(null, List.of("Can't render image: " + e.getMessage())));
        }
    }

    private Single<Void> saveImage(TileRequest tileRequest, String checksum) {
        return Single.fromCallable(() -> TileGenerator.generateImage(tileRequest))
                .flatMap(image -> driver.putObject(Render.getCacheKey(checksum), image));
    }

    private static RenderDesignResponse makeResponse(String checksum, List<String> errors) {
        return RenderDesignResponse.builder()
                .withErrors(errors)
                .withChecksum(checksum)
                .build();
    }

    private static byte[] encodeData(RenderDesignRequest request) {
        final JsonObject json = new JsonObject()
            .put("manifest", request.getManifest())
            .put("metadata", request.getMetadata())
            .put("script", request.getScript());

        return json.encode().getBytes();
    }
}
