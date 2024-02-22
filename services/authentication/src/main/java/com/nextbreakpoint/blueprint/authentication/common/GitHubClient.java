package com.nextbreakpoint.blueprint.authentication.common;

import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

import java.util.Objects;

import static com.nextbreakpoint.blueprint.common.core.ContentType.GITHUB_JSON;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;

public class GitHubClient {
    public static final String GITHUB_API_VERSION = "X-GitHub-Api-Version";
    public static final String GITHUB_API_VERSION_VALUE = "2022-11-28";

    private final WebClient webClient;

    public GitHubClient(WebClient webClient) {
        this.webClient = Objects.requireNonNull(webClient);
    }

    public Single<String> fetchUserEmail(String accessToken) {
        return webClient.get("/user/emails")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
                .putHeader(ACCEPT, GITHUB_JSON)
                .putHeader(GITHUB_API_VERSION, GITHUB_API_VERSION_VALUE)
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot retrieve user's emails", 200))
                .flatMap(response -> findPrimaryEmail(response.bodyAsJsonArray()))
                .onErrorResumeNext(this::getAuthenticationError);
    }

    public Single<JsonObject> fetchUserInfo(String accessToken) {
        return webClient.get("/user")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
                .putHeader(ACCEPT, GITHUB_JSON)
                .putHeader(GITHUB_API_VERSION, GITHUB_API_VERSION_VALUE)
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot retrieve user's details", 200))
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

    private Single<String> findPrimaryEmail(JsonArray emails) {
        return emails.stream()
                .map(email -> (JsonObject) email)
                .filter(email -> email.getBoolean("primary"))
                .map(email -> email.getString("email"))
                .findFirst()
                .map(Single::just)
                .orElseGet(() -> Single.error(Failure.accessDenied("Cannot find primary email")));
    }
}
