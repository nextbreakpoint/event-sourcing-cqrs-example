package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

import java.util.Objects;

import static com.nextbreakpoint.blueprint.common.core.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;

public class DesignsRenderClient {
    private final WebClient webClient;

    public DesignsRenderClient(WebClient webClient) {
        this.webClient = Objects.requireNonNull(webClient);
    }

    public Single<JsonObject> validateDesign(String jwtAccessToken, String payload) {
        return webClient.post("/v1/designs/validate")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(jwtAccessToken))
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSendBuffer(Buffer.buffer(payload))
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot validate design", 200))
                .map(HttpResponse::bodyAsJsonObject)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    private Single<HttpResponse<Buffer>> getSuccessfulResponseOrError(HttpResponse<Buffer> response, String message, int statusCode) {
        if (response.statusCode() != statusCode) {
            return Single.error(Failure.accessDenied(message));
        } else {
            return Single.just(response);
        }
    }

    private <R> Single<R> getAuthenticationError(Throwable throwable) {
        if (throwable instanceof Failure) {
            return Single.error(throwable);
        } else {
            return Single.error(Failure.authenticationError(throwable));
        }
    }
}
