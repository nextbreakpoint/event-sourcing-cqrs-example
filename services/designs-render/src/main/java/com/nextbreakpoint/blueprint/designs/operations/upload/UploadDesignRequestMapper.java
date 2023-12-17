package com.nextbreakpoint.blueprint.designs.operations.upload;

import com.nextbreakpoint.blueprint.common.core.Mapper;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;

public class UploadDesignRequestMapper implements Mapper<RoutingContext, UploadDesignRequest> {
    @Override
    public UploadDesignRequest transform(RoutingContext context) {
        final FileUpload fileUpload = context.fileUploads().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("the request doesn't contain the required file"));

        return UploadDesignRequest.builder()
                .withFile(fileUpload.uploadedFileName())
                .build();
    }
}
