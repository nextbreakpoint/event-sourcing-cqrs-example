package com.nextbreakpoint.shop.auth.handlers;

import com.nextbreakpoint.shop.common.vertx.Authentication;
import com.nextbreakpoint.shop.common.model.Authority;
import com.nextbreakpoint.shop.common.model.Failure;
import com.nextbreakpoint.shop.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.shop.common.vertx.WebClientFactory;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.OAuth2AuthHandler;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nextbreakpoint.shop.common.vertx.Authentication.NULL_USER_UUID;
import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static com.nextbreakpoint.shop.common.model.Headers.CONTENT_TYPE;

public class GitHubSigninHandler implements Handler<RoutingContext> {
    public static final String CALLBACK_PATH = "/auth/callback";

    private final OAuth2AuthHandler oauthHandler;
    private final WebClient accountsClient;
    private final WebClient githubClient;
    private final Set<String> adminUsers;
    private final JWTAuth jwtProvider;
    private final String cookieDomain;
    private final String webUrl;

    public GitHubSigninHandler(Vertx vertx, JsonObject config, Router router) throws MalformedURLException {
        adminUsers = config.getJsonArray("admin_users")
                .stream().map(x -> (String) x).collect(Collectors.toSet());
        cookieDomain = config.getString("cookie_domain");
        webUrl = config.getString("client_web_url");
        accountsClient = WebClientFactory.create(vertx, config.getString("server_accounts_url"), config);
        githubClient = WebClientFactory.create(vertx, config.getString("github_url"));
        jwtProvider = JWTProviderFactory.create(vertx, config);
        oauthHandler = createOAuth2Handler(vertx, config, router);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            if (routingContext.user() != null) {
                processSignin(routingContext);
            } else {
                oauthHandler.handle(routingContext);
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
                findAccount(routingContext, redirectTo, oauthAccessToken, extractPrimaryEmail(response.bodyAsJsonArray()));
            } else {
                routingContext.fail(Failure.accessDenied("Cannot retrieve user's email"));
            }
        } catch (Exception e) {
            routingContext.fail(Failure.requestFailed(e));
        }
    }

    protected void findAccount(RoutingContext routingContext, String redirectTo, String oauthAccessToken, String userEmail) {
        final String accessToken = Authentication.generateToken(jwtProvider, NULL_USER_UUID, Arrays.asList(Authority.PLATFORM));

        accountsClient.get("/api/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
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
        accountsClient.post("/api/accounts")
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(accessToken))
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSendJsonObject(makeAccount(userEmail, userInfo))
                .subscribe(response -> handleAccount(routingContext, redirectTo, response, "Cannot create account"), err -> routingContext.fail(Failure.authenticationError(err)));
    }

    protected void fetchAccount(RoutingContext routingContext, String redirectTo, String accessToken, JsonArray accounts) {
        accountsClient.get("/api/accounts/" + accounts.getString(0))
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
            final String token = Authentication.generateToken(jwtProvider, uuid, Arrays.asList(role));
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

    protected OAuth2AuthHandler createOAuth2Handler(Vertx vetx, JsonObject config, Router router) {
        final String clientId = config.getString("github_client_id");
        final String clientSecret = config.getString("github_client_secret");
        final String oauthLoginUrl = config.getString("oauth_login_url");
        final String oauthTokenPath = config.getString("oauth_token_path");
        final String oauthAuthorisePath = config.getString("oauth_authorize_path");
        final String oauthAuthority = config.getString("oauth_authority");
        final String authUrl = config.getString("client_auth_url");

        final OAuth2ClientOptions oauth2Options = new OAuth2ClientOptions()
                .setClientID(clientId)
                .setClientSecret(clientSecret)
                .setSite(oauthLoginUrl)
                .setTokenPath(oauthTokenPath)
                .setAuthorizationPath(oauthAuthorisePath);

        final OAuth2Auth oauth2Provider = OAuth2Auth.create(vetx, OAuth2FlowType.AUTH_CODE, oauth2Options);
        final OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(oauth2Provider, authUrl + CALLBACK_PATH);

        oauth2.addAuthority(oauthAuthority);
        oauth2.setupCallback(router.route(CALLBACK_PATH));

        return oauth2;
    }

    protected String extractPrimaryEmail(JsonArray emails) {
        return emails.stream()
                .map(email -> (JsonObject) email).filter(email -> email.getBoolean("primary"))
                .map(email -> email.getString("email")).findFirst().orElse(null);
    }

    protected String getAccessToken(RoutingContext routingContext) {
        return routingContext.user().principal().getString("access_token");
    }

    protected String getRedirectTo(RoutingContext routingContext) {
        return webUrl + routingContext.request().path().substring("/auth/signin".length());
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
