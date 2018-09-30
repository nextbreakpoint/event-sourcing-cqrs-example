package com.nextbreakpoint.shop.web.handlers;

import com.nextbreakpoint.shop.common.model.DesignDocument;
import com.nextbreakpoint.shop.common.vertx.Authentication;
import com.nextbreakpoint.shop.common.model.DesignResource;
import com.nextbreakpoint.shop.common.model.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpRequest;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;

public class DesignsDataHandler implements Handler<RoutingContext> {
    private final WebClient client;
    private final String webUrl;
    private final String designsUrl;

    public DesignsDataHandler(WebClient client, JsonObject config) {
        this.client = client;
        this.webUrl = config.getString("client_web_url");
        this.designsUrl = config.getString("client_designs_url");
    }

    public void handle(RoutingContext routingContext) {
        try {
            final String token = Authentication.getToken(routingContext);

            final HttpRequest<Buffer> request = client.get("/api/designs");

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
            if (response.statusCode() == 200) {
                final List<DesignResource> resources = Stream.of(response.bodyAsJson(DesignDocument[].class))
                        .map(this::makeDesign).collect(Collectors.toList());

                routingContext.put("designs", resources);
                routingContext.put("timestamp", System.currentTimeMillis());

                routingContext.next();
            } else {
                routingContext.put("designs", Collections.EMPTY_LIST);
                routingContext.put("timestamp", System.currentTimeMillis());

                routingContext.next();
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private DesignResource makeDesign(DesignDocument document) {
        return new DesignResource(document.getUuid(), document.getChecksum(), webUrl + "/content/designs/" + document.getUuid(), designsUrl + "/api/designs/" + document.getUuid() + "/0/0/0/256.png", "", "", "", "");
    }

    public static DesignsDataHandler create(WebClient client, JsonObject config) {
        return new DesignsDataHandler(client, config);
    }
}