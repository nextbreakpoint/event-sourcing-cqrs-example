package com.nextbreakpoint.blueprint.designs.operations.parse;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;

public class ParseDesignRequestMapper implements Mapper<RoutingContext, ParseDesignRequest> {
    @Override
    public ParseDesignRequest transform(RoutingContext context) {
        final FileUpload fileUpload = context.fileUploads().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("the required file is missing"));

        return new ParseDesignRequest(fileUpload.uploadedFileName());
    }
}
