package com.nextbreakpoint.shop.designs.load;

import com.nextbreakpoint.shop.common.Header;
import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.Set;

import static com.nextbreakpoint.shop.common.Headers.MODIFIED;
import static java.util.Collections.singleton;

public class LoadDesignResponseMapper implements ResponseMapper<LoadDesignResponse> {
    @Override
    public Result apply(LoadDesignResponse response) {
        final Set<Header> headers = response.getDesign()
                .map(design -> singleton(new Header(MODIFIED, design.getUpdated())))
                .orElse(Collections.emptySet());

        final String json = response.getDesign()
                .map(design -> new JsonObject()
                        .put("uuid", design.getUuid())
                        .put("json", design.getJson())
                        .put("created", design.getCreated())
                        .put("updated", design.getUpdated())
                        .encode())
                .orElse(null);

        return new Result(json, headers);
    }
}
