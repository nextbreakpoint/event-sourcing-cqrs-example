package com.nextbreakpoint.shop.common.vertx;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.List;

public class CORSHandlerFactory {
    private CORSHandlerFactory() {}

    public static CorsHandler createWithAll(String originPattern, List<String> allowedHeaders) {
        return CorsHandler.create(originPattern)
                .allowedHeaders(new HashSet<>(allowedHeaders))
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowCredentials(true);
    }

    public static CorsHandler createWithAll(String originPattern, List<String> allowedHeaders, List<String> exposedHeaders) {
        return CorsHandler.create(originPattern)
                .allowedHeaders(new HashSet<>(allowedHeaders))
                .exposedHeaders(new HashSet<>(exposedHeaders))
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowCredentials(true);
    }

    public static CorsHandler createWithGetOnly(String originPattern, List<String> headers) {
        return CorsHandler.create(originPattern)
                .allowedHeaders(new HashSet<>(headers))
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowCredentials(true);
    }
}
