package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInHandler;
import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignOutHandler;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.vertx.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;
import io.vertx.rxjava.ext.healthchecks.HealthCheckHandler;
import io.vertx.rxjava.ext.healthchecks.HealthChecks;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.OAuth2AuthHandler;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import lombok.extern.log4j.Log4j2;
import rx.Completable;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;

@Log4j2
public class Verticle extends AbstractVerticle {
    private static final String CALLBACK_PATH = "/v1/auth/callback";

    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            vertx.rxDeployVerticle(new Verticle(), new DeploymentOptions().setConfig(config))
                    .delay(30, TimeUnit.SECONDS)
                    .retry(3)
                    .subscribe(o -> log.info("Verticle deployed"), err -> log.error("Can't deploy verticle"));
        } catch (Exception e) {
            log.error("Can't start service", e);
        }
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer).toCompletable();
    }

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(config.getString("host_port"));

            final String webUrl = config.getString("client_web_url");

            final String originPattern = config.getString("origin_pattern");

            final String jksStorePath = config.getString("server_keystore_path");

            final String jksStoreSecret = config.getString("server_keystore_secret");

            final String accountsUrl = config.getString("server_accounts_url");

            final String githubUrl = config.getString("github_url");

            final String clientKeyStorePath = config.getString("client_keystore_path");

            final String clientKeyStoreSecret = config.getString("client_keystore_secret");

            final String clientTrustStorePath = config.getString("client_truststore_path");

            final String clientTrustStoreSecret = config.getString("client_truststore_secret");

            final Boolean verifyHost = Boolean.parseBoolean(config.getString("client_verify_host"));

            final String clientId = config.getString("github_client_id");

            final String clientSecret = config.getString("github_client_secret");

            final String oauthLoginUrl = config.getString("oauth_login_url");

            final String oauthTokenPath = config.getString("oauth_token_path");

            final String oauthAuthorisePath = config.getString("oauth_authorize_path");

            final String oauthAuthority = config.getString("oauth_authority");

            final String authUrl = config.getString("client_auth_url");

            final Set<String> adminUsers = config.getJsonArray("admin_users")
                    .stream()
                    .map(x -> (String) x)
                    .collect(Collectors.toSet());

            final String cookieDomain = config.getString("cookie_domain");

            final String jwtKeystoreType = config.getString("jwt_keystore_type");

            final String jwtKeystorePath = config.getString("jwt_keystore_path");

            final String jwtKeystoreSecret = config.getString("jwt_keystore_secret");

            final Router mainRouter = Router.router(vertx);

            mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
            mainRouter.route().handler(BodyHandler.create());

            final CorsHandler corsHandler = CorsHandlerFactory.createWithGetOnly(originPattern, List.of(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT));

            mainRouter.route("/*").handler(corsHandler);

            mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

            mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

            final WebClientConfig webClientConfig = WebClientConfig.builder()
                    .withVerifyHost(verifyHost)
                    .withKeyStorePath(clientKeyStorePath)
                    .withKeyStoreSecret(clientKeyStoreSecret)
                    .withTrustStorePath(clientTrustStorePath)
                    .withTrustStoreSecret(clientTrustStoreSecret)
                    .build();

            final OAuth2Options oauth2Options = new OAuth2Options()
                    .setClientID(clientId)
                    .setClientSecret(clientSecret)
                    .setSite(oauthLoginUrl)
                    .setTokenPath(oauthTokenPath)
                    .setAuthorizationPath(oauthAuthorisePath);

            final OAuth2Auth oauth2Provider = OAuth2Auth.create(vertx, oauth2Options);

            final OAuth2AuthHandler oauthHandler = OAuth2AuthHandler
                    .create(vertx, oauth2Provider, authUrl + CALLBACK_PATH)
                    .withScope(oauthAuthority)
                    .setupCallback(mainRouter.route(CALLBACK_PATH));

            final JWTProviderConfig jwtProviderConfig = JWTProviderConfig.builder()
                    .withKeyStoreType(jwtKeystoreType)
                    .withKeyStorePath(jwtKeystorePath)
                    .withKeyStoreSecret(jwtKeystoreSecret)
                    .build();

            final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, jwtProviderConfig);

            final WebClient accountsClient = WebClientFactory.create(vertx, accountsUrl, webClientConfig);

            final WebClient githubClient = WebClientFactory.create(vertx, githubUrl);

            final Handler<RoutingContext> signinHandler = createSignInHandler(cookieDomain, webUrl, adminUsers, accountsClient, githubClient, jwtProvider, oauthHandler);

            final Handler<RoutingContext> signoutHandler = createSignOutHandler(cookieDomain, webUrl);

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("signIn")
                                .handler(context -> signinHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("signOut")
                                .handler(context -> signoutHandler.handle(RoutingContext.newInstance(context)));

                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.mountSubRouter("/v1", apiRouter);

                        mainRouter.get("/v1/apidocs").handler(apiV1DocsHandler);

                        mainRouter.get("/health*").handler(healthCheckHandler);

                        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                        mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

                        mainRouter.route().failureHandler(routingContext -> redirectOnFailure(routingContext, webUrl));

                        final ServerConfig serverConfig = ServerConfig.builder()
                                .withJksStorePath(jksStorePath)
                                .withJksStoreSecret(jksStoreSecret)
                                .build();

                        final HttpServerOptions options = Server.makeOptions(serverConfig);

                        vertx.createHttpServer(options)
                                .requestHandler(mainRouter)
                                .rxListen(port)
                                .doOnSuccess(result -> log.info("Service listening on port " + port))
                                .doOnError(err -> log.error("Can't create server", err))
                                .subscribe(result -> promise.complete(), promise::fail);
                    })
                    .onFailure(err -> {
                        log.error("Can't create router", err);
                        promise.fail(err);
                    });
        } catch (Exception e) {
            log.error("Failed to start server", e);
            promise.fail(e);
        }
    }

    private void redirectOnFailure(RoutingContext routingContext, String webUrl) {
        ResponseHelper.redirectToError(routingContext, statusCode -> webUrl + "/error/" + statusCode);
    }

    protected Handler<RoutingContext> createSignInHandler(String cookieDomain, String webUrl, Set<String> adminUsers, WebClient accountsClient, WebClient githubClient, JWTAuth jwtProvider, OAuth2AuthHandler oauthHandler) {
        return new GitHubSignInHandler(cookieDomain, webUrl, adminUsers, accountsClient, githubClient, jwtProvider, oauthHandler);
    }

    protected Handler<RoutingContext> createSignOutHandler(String cookieDomain, String webUrl) {
        return new GitHubSignOutHandler(cookieDomain, webUrl);
    }
}