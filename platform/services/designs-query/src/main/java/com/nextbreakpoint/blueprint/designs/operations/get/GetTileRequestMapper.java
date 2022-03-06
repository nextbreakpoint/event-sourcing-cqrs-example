package com.nextbreakpoint.blueprint.designs.operations.get;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.UUID;

public class GetTileRequestMapper implements Mapper<RoutingContext, GetTileRequest> {
    @Override
    public GetTileRequest transform(RoutingContext context) {
        final HttpServerRequest request = context.request();

        final String uuidParam = request.getParam("designId");

        if (uuidParam == null) {
            throw new IllegalStateException("parameter designId missing from routing context");
        }

        final String levelParam = request.getParam("level");

        if (levelParam == null) {
            throw new IllegalStateException("parameter level missing from routing context");
        }

        final String colParam = request.getParam("col");

        if (colParam == null) {
            throw new IllegalStateException("parameter col missing from routing context");
        }

        final String rowParam = request.getParam("row");

        if (rowParam == null) {
            throw new IllegalStateException("parameter row missing from routing context");
        }

        final String sizePram = request.getParam("size");

        if (sizePram == null) {
            throw new IllegalStateException("parameter size missing from routing context");
        }

        final String draftParam = request.getParam("draft", "false");

        try {
            final UUID uuid = UUID.fromString(uuidParam);
            final int level = Integer.parseInt(levelParam);
            final int row = Integer.parseInt(rowParam);
            final int col = Integer.parseInt(colParam);
            final int size = Integer.parseInt(sizePram);
            final boolean draft = Boolean.parseBoolean(draftParam);
            return new GetTileRequest(uuid, level, row, col, size, draft);
        } catch (Exception e) {
            throw new IllegalStateException("invalid parameters: " + e.getMessage());
        }
    }
}
