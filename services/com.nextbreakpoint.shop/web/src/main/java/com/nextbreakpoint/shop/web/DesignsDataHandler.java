package com.nextbreakpoint.shop.web;

import com.nextbreakpoint.shop.common.Authentication;
import com.nextbreakpoint.shop.common.Design;
import com.nextbreakpoint.shop.common.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpRequest;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.nextbreakpoint.shop.common.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.Headers.MODIFIED;
import static com.nextbreakpoint.shop.common.TimeUtil.TIMESTAMP_PATTERN;

public class DesignsDataHandler implements Handler<RoutingContext> {
    private final WebClient client;
    private final String webUrl;
    private final String designsUrl;

    public DesignsDataHandler(WebClient client, JsonObject config) {
        this.client = client;
        this.webUrl = config.getString("web_url");
        this.designsUrl = config.getString("designs_url");
    }

    public void handle(RoutingContext routingContext) {
        try {
            final String token = Authentication.getToken(routingContext);

            final HttpRequest<Buffer> request = client.get("/designs");

            if (token != null) {
                request.putHeader(AUTHORIZATION, token);
            }

            request.putHeader(ACCEPT, APPLICATION_JSON).rxSend()
                    .subscribe(response -> handleDesigns(routingContext, response), e -> routingContext.fail(Failure.requestFailed(e)));
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void handleDesigns(RoutingContext routingContext, HttpResponse<Buffer> response) {
        try {
            final List<Design> objects = (List<Design>) response.bodyAsJson(List.class).stream().map(uuid -> makeDesign((String)uuid)).collect(Collectors.toList());

            final String modified = response.getHeader(MODIFIED);

            final SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);

            routingContext.put("modified", df.format(new Date(Long.parseLong(modified))));

            routingContext.put("offset", modified);

            routingContext.put("designs", objects);

            routingContext.next();
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private Design makeDesign(String uuid) {
        return new Design(uuid, webUrl + "/content/designs/" + uuid, designsUrl + "/designs/" + uuid + "/0/0/0/256.png", "", "", "", "", "");
    }

    public static DesignsDataHandler create(WebClient client, JsonObject config) {
        return new DesignsDataHandler(client, config);
    }
}