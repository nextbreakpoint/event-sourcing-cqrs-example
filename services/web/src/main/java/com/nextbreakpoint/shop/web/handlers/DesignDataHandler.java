package com.nextbreakpoint.shop.web.handlers;

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

import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;

public class DesignDataHandler implements Handler<RoutingContext> {
    private final WebClient client;
    private final String webUrl;
    private final String designsUrl;

    public DesignDataHandler(WebClient client, JsonObject config) {
        this.client = client;
        this.webUrl = config.getString("client_web_url");
        this.designsUrl = config.getString("client_designs_url");
    }

    public void handle(RoutingContext routingContext) {
        try {
            final String token = Authentication.getToken(routingContext);

            final String uuid = routingContext.pathParam("param0");

            final HttpRequest<Buffer> request = client.get("/api/designs/" + uuid);

            if (token != null) {
                request.putHeader(AUTHORIZATION, token);
            }

            request.putHeader(ACCEPT, APPLICATION_JSON).rxSend()
                  .subscribe(response -> handleDesign(routingContext, response), e -> routingContext.fail(Failure.requestFailed(e)));
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private void handleDesign(RoutingContext routingContext, HttpResponse<Buffer> response) {
        try {
            final JsonObject jsonObject = response.bodyAsJsonObject();

            final String uuid = jsonObject.getString("uuid");
            final String updated = jsonObject.getString("updated");
            final String created = jsonObject.getString("created");

            final JsonObject design = new JsonObject(jsonObject.getString("json"));

            final String manifest = design.getString("manifest");
            final String metadata = design.getString("metadata");
            final String script = design.getString("script");

            final DesignResource object = makeDesign(uuid, created, updated, manifest, metadata, script);

            routingContext.put("design", object);
            routingContext.put("timestamp", System.currentTimeMillis());

            routingContext.next();
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    private DesignResource makeDesign(String uuid, String created, String modified, String manifest, String metadata, String script) {
        return new DesignResource(uuid, webUrl + "/content/designs/" + uuid, designsUrl + "/api/designs/" + uuid + "/0/0/0/512.png", created, modified, manifest, metadata, script);
    }

    public static DesignDataHandler create(WebClient client, JsonObject config) {
        return new DesignDataHandler(client, config);
    }
}