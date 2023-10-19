package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.nextfractal.core.common.Bundle;
import com.nextbreakpoint.nextfractal.core.common.TileGenerator;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Promise;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TileRenderer {
    public void renderImage(TileRenderRequested event, Promise<Result> promise) {
        try {
            final Params params = makeTileParams(event);
            final JsonObject json = new JsonObject(event.getData());
            final Bundle bundle = convertToBundle(json);
            final byte[] image = renderImage(bundle, params);
            log.debug("Image size {}", image.length);
            promise.complete(Result.of(image, null));
        } catch (Exception e) {
            log.error("Can't render image", e);
            promise.complete(Result.of(new byte[0], e));
        }
    }

    private static byte[] renderImage(Bundle bundle, Params params) throws Exception {
        int side = 1 << params.getLevel();
        return TileGenerator.generateImage(TileGenerator.createTileRequest(params.getSize(), side, side, params.getRow() % side, params.getCol() % side, bundle));
    }

    private static Params makeTileParams(TileRenderRequested event) {
        final int zoom = event.getLevel();
        final int row = event.getRow();
        final int col = event.getCol();
        final int size = 256;
        return new Params(zoom, row, col, size);
    }

    private static Bundle convertToBundle(JsonObject jsonObject) throws Exception {
        final String manifest = jsonObject.getString("manifest");
        final String metadata = jsonObject.getString("metadata");
        final String script = jsonObject.getString("script");
        return BundleUtils.createBundle(manifest, metadata, script);
    }

    private static class Params {
        private final int level;
        private final int row;
        private final int col;
        private final int size;

        public Params(int level, int row, int col, int size) {
            this.level = level;
            this.col = col;
            this.row = row;
            this.size = size;
        }

        public int getLevel() {
            return level;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public int getSize() {
            return size;
        }
    }
}
