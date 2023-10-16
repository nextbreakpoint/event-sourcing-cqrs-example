package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import com.nextbreakpoint.blueprint.common.vertx.Failure;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.extern.log4j.Log4j2;

import java.util.*;

import static com.nextbreakpoint.blueprint.common.core.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.common.vertx.Authentication.NULL_USER_UUID;

@Log4j2
public class GitHubSignInHandler implements Handler<RoutingContext> {
    private final OAuth2Auth oauthHandler;
    private String oauthAuthority;
    private String callbackPath;
    private final WebClient accountsClient;
    private final WebClient githubClient;
    private final JWTAuth jwtProvider;

    private final Set<String> adminUsers;
    private final String cookieDomain;
    private final String authUrl;
    private final String webUrl;

    public GitHubSignInHandler(String cookieDomain, String webUrl, String authUrl, Set<String> adminUsers, WebClient accountsClient, WebClient githubClient, JWTAuth jwtProvider, OAuth2Auth oauthHandler, String oauthAuthority, String callbackPath) {
        this.cookieDomain = Objects.requireNonNull(cookieDomain);
        this.webUrl = Objects.requireNonNull(webUrl);
        this.authUrl = Objects.requireNonNull(authUrl);
        this.adminUsers = Objects.requireNonNull(adminUsers);
        this.accountsClient = Objects.requireNonNull(accountsClient);
        this.githubClient = Objects.requireNonNull(githubClient);
        this.jwtProvider = Objects.requireNonNull(jwtProvider);
        this.oauthHandler = Objects.requireNonNull(oauthHandler);
        this.oauthAuthority = Objects.requireNonNull(oauthAuthority);
        this.callbackPath = Objects.requireNonNull(callbackPath);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            if (routingContext.user() != null) {
                processSignin(routingContext);
            } else {
                String authorizationURL = oauthHandler.authorizeURL(new OAuth2AuthorizationURL(new JsonObject()
                        .put("redirect_uri", authUrl + callbackPath)
                        .put("scope", oauthAuthority)
                        .put("state", routingContext.request().uri())
                ));

                routingContext.response()
                        .putHeader("Location", authorizationURL)
                        .setStatusCode(302)
                        .end();
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void processSignin(RoutingContext routingContext) {
        try {
            final String oauthAccessToken = getAccessToken(routingContext);
            if (oauthAccessToken != null) {
                fetchUserEmail(routingContext, oauthAccessToken, getRedirectTo(routingContext));
            } else {
                routingContext.fail(Failure.accessDenied("Missing OAuth access token"));
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void fetchUserEmail(RoutingContext routingContext, String oauthAccessToken, String redirectTo) {
        githubClient.get("/user/emails")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(oauthAccessToken))
                .rxSend()
                .subscribe(response -> handleUserEmail(routingContext, redirectTo, oauthAccessToken, response), err -> routingContext.fail(Failure.authenticationError(err)));
    }

    protected void handleUserEmail(RoutingContext routingContext, String redirectTo, String oauthAccessToken, HttpResponse<Buffer> response) {
        try {
            if (response.statusCode() == 200) {
                final String userEmail = extractPrimaryEmail(response.bodyAsJsonArray());
                log.info("User email: " + userEmail);
                findAccount(routingContext, redirectTo, oauthAccessToken, userEmail);
            } else {
                routingContext.fail(Failure.accessDenied("Cannot retrieve user's email"));
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void findAccount(RoutingContext routingContext, String redirectTo, String oauthAccessToken, String userEmail) {
        final String accessToken = Authentication.generateToken(jwtProvider, NULL_USER_UUID, Arrays.asList(Authority.PLATFORM));

        accountsClient.get("/v1/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .addQueryParam("email", userEmail)
                .rxSend()
                .subscribe(response -> handleFoundAccount(routingContext, redirectTo, userEmail, accessToken, oauthAccessToken, response), err -> routingContext.fail(Failure.authenticationError(err)));
    }

    protected void handleFoundAccount(RoutingContext routingContext, String redirectTo, String userEmail, String accessToken, String oauthAccessToken, HttpResponse<Buffer> response) {
        try {
            if (response.statusCode() == 200) {
                fetchOrCreateAccount(routingContext, redirectTo, accessToken, oauthAccessToken, userEmail, response.bodyAsJsonArray());
            } else {
                routingContext.fail(Failure.accessDenied("Cannot find user account"));
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void fetchOrCreateAccount(RoutingContext routingContext, String redirectTo, String accessToken, String oauthAccessToken, String userEmail, JsonArray accounts) {
        if (accounts.size() == 1) {
            fetchAccount(routingContext, redirectTo, accessToken, accounts);
        } else {
            fetchUserInfo(routingContext, redirectTo, accessToken, oauthAccessToken, userEmail);
        }
    }

    protected void fetchUserInfo(RoutingContext routingContext, String redirectTo, String accessToken, String oauthAccessToken, String userEmail) {
        githubClient.get("/user")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(oauthAccessToken))
                .rxSend()
                .subscribe(response -> handleUserInfo(routingContext, redirectTo, userEmail, accessToken, response), err -> routingContext.fail(Failure.authenticationError(err)));
    }

    protected void handleUserInfo(RoutingContext routingContext, String redirectTo, String userEmail, String accessToken, HttpResponse<Buffer> response) {
        try {
            if (response.statusCode() == 200) {
                createAccount(routingContext, redirectTo, accessToken, userEmail, response.bodyAsJsonObject());
            } else {
                routingContext.fail(Failure.accessDenied("Cannot retrieve user's details"));
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void createAccount(RoutingContext routingContext, String redirectTo, String accessToken, String userEmail, JsonObject userInfo) {
        JsonObject account = makeAccount(userEmail, userInfo);
        log.info("User account: " + account.encode());
        accountsClient.post("/v1/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSendJsonObject(account)
                .subscribe(response -> handleAccount(routingContext, redirectTo, response, "Cannot create account"), err -> routingContext.fail(Failure.authenticationError(err)));
    }

    protected void fetchAccount(RoutingContext routingContext, String redirectTo, String accessToken, JsonArray accounts) {
        accountsClient.get("/v1/accounts/" + accounts.getString(0))
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSend()
                .subscribe(response -> handleAccount(routingContext, redirectTo, response, "Cannot fetch account"), err -> routingContext.fail(Failure.authenticationError(err)));
    }

    protected void handleAccount(RoutingContext routingContext, String redirectTo, HttpResponse<Buffer> response, String errorMessage) {
        try {
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                processAccount(routingContext, redirectTo, response.bodyAsJsonObject());
            } else {
                routingContext.fail(Failure.accessDenied(errorMessage));
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void processAccount(RoutingContext routingContext, String redirectTo, JsonObject account) {
        final String uuid = account.getString("uuid");
        final String role = account.getString("role");
        if (uuid != null && role != null) {
            log.info("User role: " + role);
            final String token = Authentication.generateToken(jwtProvider, uuid, List.of(role));
            sendRedirectResponse(routingContext, redirectTo, Authentication.createCookie(token, cookieDomain));
        } else {
            routingContext.fail(Failure.accessDenied("Missing account's uuid or role"));
        }
    }

    protected void sendRedirectResponse(RoutingContext routingContext, String redirectTo, Cookie cookie) {
        routingContext.response()
                .putHeader("Set-Cookie", cookie.encode())
                .putHeader("Location", redirectTo)
                .setStatusCode(303)
                .end();
    }

    protected String extractPrimaryEmail(JsonArray emails) {
        return emails.stream()
                .map(email -> (JsonObject) email)
                .filter(email -> email.getBoolean("primary"))
                .map(email -> email.getString("email"))
                .findFirst()
                .orElse(null);
    }

    protected String getAccessToken(RoutingContext routingContext) {
        return routingContext.user().principal().getString("access_token");
    }

    protected String getRedirectTo(RoutingContext routingContext) {
        return webUrl + routingContext.request().path().substring("/v1/auth/signin".length());
    }

    protected JsonObject makeAccount(String userEmail, JsonObject userInfo) {
        return new JsonObject()
                .put("email", userEmail)
                .put("name", userInfo.getString("name"))
                .put("role", getAuthority(userEmail));
    }

    protected String getAuthority(String userEmail) {
        return adminUsers.contains(userEmail) ? Authority.ADMIN : Authority.GUEST;
    }
}
