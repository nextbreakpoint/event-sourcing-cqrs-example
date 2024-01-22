package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.TileGenerator;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TileRenderer {
    public Result renderImage(TileRenderRequested event) {
        try {
            final TileParams params = makeTileParams(event);
            final JsonObject json = new JsonObject(event.getData());
            final Bundle bundle = convertToBundle(json);
            final byte[] image = renderImage(bundle, params);
            log.debug("Image size {}", image.length);
            return Result.of(image, null);
        } catch (Exception e) {
            log.error("Can't render image", e);
            return Result.of(new byte[0], e);
        }
    }

    private static byte[] renderImage(Bundle bundle, TileParams params) throws Exception {
        int side = 1 << params.getLevel();
        return TileGenerator.generateImage(TileGenerator.createTileRequest(params.getSize(), side, side, params.getRow() % side, params.getCol() % side, bundle));
    }

    private static TileParams makeTileParams(TileRenderRequested event) {
        final int zoom = event.getLevel();
        final int row = event.getRow();
        final int col = event.getCol();
        final int size = 256;
        return new TileParams(zoom, row, col, size);
    }

    private static Bundle convertToBundle(JsonObject jsonObject) throws Exception {
        final String manifest = jsonObject.getString("manifest");
        final String metadata = jsonObject.getString("metadata");
        final String script = jsonObject.getString("script");
        return BundleUtils.createBundle(manifest, metadata, script).orThrow();
    }
}
