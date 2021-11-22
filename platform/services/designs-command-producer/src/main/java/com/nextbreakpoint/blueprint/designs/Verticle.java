package com.nextbreakpoint.blueprint.designs;

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
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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
        try (FileInputStream stream = new FileInputStream(configPath)) {
            return new JsonObject(IOUtils.toString(stream));
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

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String eventsTopic = environment.resolve(config.getString("events_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

            final KafkaProducer<String, String> kafkaProducer = KafkaClientFactory.createProducer(environment, vertx, config);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext> insertDesignHandler = new AccessHandler(jwtProvider, Factory.createInsertDesignHandler(kafkaProducer, eventsTopic, messageSource), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> updateDesignHandler = new AccessHandler(jwtProvider, Factory.createUpdateDesignHandler(kafkaProducer, eventsTopic, messageSource), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> deleteDesignHandler = new AccessHandler(jwtProvider, Factory.createDeleteDesignHandler(kafkaProducer, eventsTopic, messageSource), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final String url = resource.toURI().toString();

            RouterBuilder.create(vertx.getDelegate(), url)
                .onSuccess(routerBuilder -> {
                    routerBuilder.operation("insertDesign")
                            .handler(context -> insertDesignHandler.handle(RoutingContext.newInstance(context)));

                    routerBuilder.operation("updateDesign")
                            .handler(context -> updateDesignHandler.handle(RoutingContext.newInstance(context)));

                    routerBuilder.operation("deleteDesign")
                            .handler(context -> deleteDesignHandler.handle(RoutingContext.newInstance(context)));

                    final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                    mainRouter.route().handler(MDCHandler.create());
                    mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
                    mainRouter.route().handler(BodyHandler.create());
                    //mainRouter.route().handler(CookieHandler.create());
                    mainRouter.route().handler(TimeoutHandler.create(30000));

                    mainRouter.route("/*").handler(corsHandler);

                    mainRouter.mountSubRouter("/v1", apiRouter);

                    mainRouter.get("/v1/apidocs").handler(apiV1DocsHandler);

                    mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                    mainRouter.route().failureHandler(ResponseHelper::sendFailure);

                    final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

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
}
