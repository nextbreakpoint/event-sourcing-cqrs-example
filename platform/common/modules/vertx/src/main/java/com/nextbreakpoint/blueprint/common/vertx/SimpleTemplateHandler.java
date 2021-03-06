package com.nextbreakpoint.blueprint.common.vertx;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.common.template.TemplateEngine;

import java.util.HashMap;

public class SimpleTemplateHandler implements Handler<RoutingContext> {
    private final TemplateEngine engine;
    private final String templateDirectory;
    private final String contentType;
    private final String fileName;

    public SimpleTemplateHandler(TemplateEngine engine, String templateDirectory, String contentType, String fileName) {
        this.engine = engine;
        this.templateDirectory = templateDirectory;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    public void handle(RoutingContext context) {
        engine.rxRender(new HashMap<>(), templateDirectory + "/" + fileName)
                .subscribe(buffer -> emitResponse(context, buffer), err -> context.fail(err));
    }

    private void emitResponse(RoutingContext context, Buffer buffer) {
        context.response().putHeader(HttpHeaders.CONTENT_TYPE.toString(), contentType).end(buffer);
    }

    public static Handler<RoutingContext> create(TemplateEngine engine, String templateDirectory, String contentType, String fileName) {
        return new SimpleTemplateHandler(engine, templateDirectory, contentType, fileName);
    }
}
