package com.nextbreakpoint.blueprint.authentication.common;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

import java.util.Objects;
import java.util.Set;

import static com.nextbreakpoint.blueprint.common.core.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;

public class AccountsClient {
    private final WebClient webClient;
    private final Set<String> adminUsers;

    public AccountsClient(WebClient webClient, Set<String> adminUsers) {
        this.webClient = Objects.requireNonNull(webClient);
        this.adminUsers = Objects.requireNonNull(adminUsers);
    }

    public Single<JsonArray> findAccounts(String jwtAccessToken, String email) {
        return webClient.get("/v1/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(jwtAccessToken))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .addQueryParam("email", email)
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot find account", 200))
                .map(HttpResponse::bodyAsJsonArray)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    public Single<JsonObject> createAccount(String jwtAccessToken, String email, String name) {
        return webClient.post("/v1/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(jwtAccessToken))
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSendJsonObject(makeAccount(email, name))
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot create account", 201))
                .map(HttpResponse::bodyAsJsonObject)
                .onErrorResumeNext(this::getAuthenticationError);
    }

    public Single<JsonObject> fetchAccount(String jwtAccessToken, String accountId) {
        return webClient.get("/v1/accounts/" + accountId)
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(jwtAccessToken))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSend()
                .flatMap(response -> getSuccessfulResponseOrError(response, "Cannot fetch account", 200))
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

    private JsonObject makeAccount(String email, String name) {
        return new JsonObject()
                .put("email", email)
                .put("name", name)
                .put("role", getAuthority(email));
    }

    private String getAuthority(String userEmail) {
        return adminUsers.contains(userEmail) ? Authority.ADMIN : Authority.GUEST;
    }
}
