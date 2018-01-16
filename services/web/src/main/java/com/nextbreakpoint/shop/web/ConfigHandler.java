package com.nextbreakpoint.shop.web;

import com.nextbreakpoint.shop.common.ContentType;
import com.nextbreakpoint.shop.common.Failure;
import com.nextbreakpoint.shop.common.Headers;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ConfigHandler implements Handler<RoutingContext> {
    private static final int EXPIRY_TIME_IN_SECONDS = 60;

    private final JsonObject webConfig;

    public ConfigHandler(JsonObject config) {
        webConfig = config;
    }

    public void handle(RoutingContext routingContext) {
        try {
            final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            final Date now = Calendar.getInstance().getTime();
            final Date expiry = new Date(now.getTime() + EXPIRY_TIME_IN_SECONDS * 1000);
            routingContext.response().setChunked(true);
            routingContext.response().putHeader(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                    .putHeader("Cache-Control", "public, max-age:" + EXPIRY_TIME_IN_SECONDS)
                    .putHeader("Last-Modified", df.format(now) + " GMT")
                    .putHeader("Expires", df.format(expiry) + " GMT")
                    .setStatusCode(200)
                    .write(webConfig.encode()).end();
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    public static ConfigHandler create(JsonObject config) {
        return new ConfigHandler(config);
    }
}