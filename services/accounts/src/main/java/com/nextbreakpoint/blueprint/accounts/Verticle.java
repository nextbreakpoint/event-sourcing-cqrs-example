package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.accounts.persistence.MySQLStore;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.vertx.AccessHandler;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.Initializer;
import com.nextbreakpoint.blueprint.common.vertx.JDBCClientConfig;
import com.nextbreakpoint.blueprint.common.vertx.JDBCClientFactory;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderConfig;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.blueprint.common.vertx.OpenApiHandler;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.Server;
import com.nextbreakpoint.blueprint.common.vertx.ServerConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.healthchecks.HealthCheckHandler;
import io.vertx.rxjava.ext.healthchecks.HealthChecks;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import lombok.extern.log4j.Log4j2;
import rx.Completable;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Authority.GUEST;
import static com.nextbreakpoint.blueprint.common.core.Authority.PLATFORM;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.COOKIE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;

@Log4j2
public class Verticle extends AbstractVerticle {
    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            vertx.rxDeployVerticle(new Verticle(), new DeploymentOptions().setConfig(config))
                    .delay(5, TimeUnit.SECONDS)
                    .retry(3)
                    .subscribe(o -> log.info("Verticle deployed"), err -> log.error("Can't deploy verticle", err));
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

            final String originPattern = config.getString("origin_pattern");

            final String jksStorePath = config.getString("server_keystore_path");

            final String jksStoreSecret = config.getString("server_keystore_secret");

            final String jdbcUrl = config.getString("jdbc_url", "jdbc:hsqldb:mem:test?shutdown=true");

            final String jdbcDriver = config.getString("jdbc_driver", "org.hsqldb.jdbcDriver");

            final String jdbcUsername = config.getString("jdbc_username", "root");

            final String jdbcPassword = config.getString("jdbc_password", "root");

            final int jdbcMaxPoolSize = Integer.parseInt(config.getString("jdbc_max_pool_size", "200"));

            final int jdbcMinPoolSize = Integer.parseInt(config.getString("jdbc_min_pool_size", "20"));

            final String jwtKeystoreType = config.getString("jwt_keystore_type");

            final String jwtKeystorePath = config.getString("jwt_keystore_path");

            final String jwtKeystoreSecret = config.getString("jwt_keystore_secret");

            final JWTProviderConfig jwtProviderConfig = JWTProviderConfig.builder()
                    .withKeyStoreType(jwtKeystoreType)
                    .withKeyStorePath(jwtKeystorePath)
                    .withKeyStoreSecret(jwtKeystoreSecret)
                    .build();

            final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, jwtProviderConfig);

            final JDBCClientConfig jdbcConfig = JDBCClientConfig.builder()
                    .withUrl(jdbcUrl)
                    .withDriver(jdbcDriver)
                    .withUsername(jdbcUsername)
                    .withPassword(jdbcPassword)
                    .withMaxPoolSize(jdbcMaxPoolSize)
                    .withMinPoolSize(jdbcMinPoolSize)
                    .build();

            final JDBCClient jdbcClient = JDBCClientFactory.create(vertx, jdbcConfig);

            final Store store = new MySQLStore(jdbcClient);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, List.of(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.response().setStatusCode(403).setStatusMessage("Access denied").end();

            final Handler<RoutingContext> listAccountsHandler = new AccessHandler(jwtProvider, Factory.createListAccountsHandler(store), onAccessDenied, List.of(ADMIN, PLATFORM));

            final Handler<RoutingContext> loadSelfAccountHandler = new AccessHandler(jwtProvider, Factory.createLoadSelfAccountHandler(store), onAccessDenied, List.of(ADMIN, GUEST));

            final Handler<RoutingContext> loadAccountHandler = new AccessHandler(jwtProvider, Factory.createLoadAccountHandler(store), onAccessDenied, List.of(ADMIN, PLATFORM));

            final Handler<RoutingContext> insertAccountHandler = new AccessHandler(jwtProvider, Factory.createInsertAccountHandler(store), onAccessDenied, List.of(ADMIN, PLATFORM));

            final Handler<RoutingContext> deleteAccountHandler = new AccessHandler(jwtProvider, Factory.createDeleteAccountHandler(store), onAccessDenied, List.of(ADMIN));

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            final Handler<RoutingContext> metricsHandler = PrometheusScrapingHandler.create();

            healthCheckHandler.register("database-table-account", 2000, future -> checkTable(store, future, "ACCOUNT"));

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("apidocs")
                                .handler(context -> apiV1DocsHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("health")
                                .handler(context -> healthCheckHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("metrics")
                                .handler(context -> metricsHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("listAccounts")
                                .handler(context -> listAccountsHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("loadSelfAccount")
                                .handler(context -> loadSelfAccountHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("loadAccount")
                                .handler(context -> loadAccountHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("insertAccount")
                                .handler(context -> insertAccountHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("deleteAccount")
                                .handler(context -> deleteAccountHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("selfAccountOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        routerBuilder.operation("accountOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        routerBuilder.operation("accountsOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        final Router router = Router.newInstance(routerBuilder.createRouter());

                        final Router mainRouter = Router.router(vertx);

                        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
                        mainRouter.route().handler(TimeoutHandler.create(10000));
                        mainRouter.route().handler(corsHandler);
                        mainRouter.route().handler(BodyHandler.create());
                        mainRouter.route("/*").subRouter(router);
                        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

                        final ServerConfig serverConfig = ServerConfig.builder()
                                .withJksStorePath(jksStorePath)
                                .withJksStoreSecret(jksStoreSecret)
                                .build();

                        final HttpServerOptions options = Server.makeOptions(serverConfig);

                        vertx.createHttpServer(options)
                                .requestHandler(mainRouter)
                                .rxListen(port)
                                .subscribe(result -> {
                                    log.info("Service listening on port {}", port);
                                    promise.complete();
                                }, err -> {
                                    log.error("Can't create server", err);
                                    promise.fail(err);
                                });
                    })
                    .onFailure(err -> {
                        log.error("Can't create router", err);
                        promise.fail(err);
                    });
        } catch (Exception e) {
            log.error("Failed to initialize service", e);
            promise.fail(e);
        }
    }

    private void checkTable(Store store, Promise<Status> promise, String tableName) {
        store.existsTable(tableName)
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(exists -> promise.complete(exists ? Status.OK() : Status.KO()), err -> promise.complete(Status.KO()));
    }
}
