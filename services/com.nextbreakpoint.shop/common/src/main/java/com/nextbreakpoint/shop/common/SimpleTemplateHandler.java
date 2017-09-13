package com.nextbreakpoint.shop.common;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.templ.TemplateEngine;

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
        engine.rxRender(context, templateDirectory + "/" + fileName)
                .subscribe(buffer -> context.response().putHeader(HttpHeaders.CONTENT_TYPE.toString(), contentType).end(buffer), err -> context.fail(err));
    }

    public static Handler<RoutingContext> create(TemplateEngine engine, String templateDirectory, String contentType, String fileName) {
        return new SimpleTemplateHandler(engine, templateDirectory, contentType, fileName);
    }
}
