package com.nextbreakpoint.shop.designs;

import com.nextbreakpoint.nextfractal.core.Bundle;
import com.nextbreakpoint.nextfractal.core.TileUtils;
import io.vertx.core.json.JsonObject;
import rx.Single;

public class BundleUtil {
    private BundleUtil() {}

    public static Single<Bundle> parseBundle(JsonObject object) {
        final String manifest = object.getString("manifest");
        final String metadata = object.getString("metadata");
        final String script = object.getString("script");
        return Single.fromCallable(() -> TileUtils.parseData(manifest, metadata, script));
    }
}
