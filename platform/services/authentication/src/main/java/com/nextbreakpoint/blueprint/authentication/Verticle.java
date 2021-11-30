package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignInHandler;
import com.nextbreakpoint.blueprint.authentication.handlers.GitHubSignOutHandler;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.vertx.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    public static void main(String[] args) {
        try {
            final JsonObject config = loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions().setEnabled(true);

            final MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                    .setPrometheusOptions(prometheusOptions).setEnabled(true);

            final OpenTracingOptions tracingOptions = new OpenTracingOptions();

            final AddressResolverOptions addressResolverOptions = new AddressResolverOptions()
                    .setCacheNegativeTimeToLive(0)
                    .setCacheMaxTimeToLive(30);

            final VertxOptions vertxOptions = new VertxOptions()
                    .setAddressResolverOptions(addressResolverOptions)
                    .setMetricsOptions(metricsOptions)
                    .setTracingOptions(tracingOptions);

            final Vertx vertx = Vertx.vertx(vertxOptions);

            vertx.deployVerticle(new Verticle(), new DeploymentOptions().setConfig(config));
        } catch (Exception e) {
            logger.error("Can't start service", e);
        }
    }

    private static JsonObject loadConfig(String configPath) throws IOException {
        final Environment environment = Environment.getDefaultEnvironment();

        try (FileInputStream stream = new FileInputStream(configPath)) {
            return new JsonObject(environment.resolve(IOUtils.toString(stream)));
        }
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer)
                .doOnError(err -> logger.error("Failed to start server", err))
                .toCompletable();
    }

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(config.getString("host_port"));

            final String webUrl = config.getString("client_web_url");

            final String originPattern = config.getString("origin_pattern");

            final Router mainRouter = Router.router(vertx);

            mainRouter.route().handler(MDCHandler.create());
            mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
            //mainRouter.route().handler(CookieHandler.create());
            mainRouter.route().handler(BodyHandler.create());

            final CorsHandler corsHandler = CorsHandlerFactory.createWithGetOnly(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT));

            mainRouter.route("/*").handler(corsHandler);

            mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

            mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

            final Handler<RoutingContext> signinHandler = createSignInHandler(config, mainRouter);

            final Handler<RoutingContext> signoutHandler = createSignOutHandler(config, mainRouter);

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final String specUri = resource.toURI().toString();

            RouterBuilder.create(vertx.getDelegate(), specUri)
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("signIn")
                                .handler(context -> signinHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("signOut")
                                .handler(context -> signoutHandler.handle(RoutingContext.newInstance(context)));

                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.mountSubRouter("/v1", apiRouter);

                        mainRouter.get("/v1/apidocs").handler(apiV1DocsHandler);

                        mainRouter.route().failureHandler(routingContext -> redirectOnFailure(routingContext, webUrl));

                        final HttpServerOptions options = ServerUtil.makeServerOptions(config);

                        vertx.createHttpServer(options)
                                .requestHandler(mainRouter)
                                .rxListen(port)
                                .doOnSuccess(result -> logger.info("Service listening on port " + port))
                                .doOnError(err -> logger.error("Can't create server", err))
                                .subscribe(result -> promise.complete(), promise::fail);
                    })
                    .onFailure(err -> {
                        logger.error("Can't create router", err);
                        promise.fail(err);
                    });
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            promise.fail(e);
        }
    }

    private void redirectOnFailure(RoutingContext routingContext, String webUrl) {
        ResponseHelper.redirectToError(routingContext, statusCode -> webUrl + "/error/" + statusCode);
    }

    protected Handler<RoutingContext> createSignInHandler(JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSignInHandler(vertx, config, router);
    }

    protected Handler<RoutingContext> createSignOutHandler(JsonObject config, Router router) throws MalformedURLException {
        return new GitHubSignOutHandler(vertx, config, router);
    }
}
