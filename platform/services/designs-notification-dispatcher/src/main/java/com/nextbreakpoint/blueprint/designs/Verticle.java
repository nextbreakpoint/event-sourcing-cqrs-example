package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.NotificationDispatcher;
import com.nextbreakpoint.blueprint.designs.operations.DesignAggregateUpdateCompletedController;
import com.nextbreakpoint.blueprint.designs.operations.TileAggregateUpdateCompletedController;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
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
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nextbreakpoint.blueprint.common.core.Authority.*;
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

    @Override
    public Completable rxStop() {
        return Completable.fromCallable(() -> {
            if (pollingThread != null) {
                try {
                    pollingThread.interrupt();
                    pollingThread.join();
                } catch (InterruptedException e) {
                    logger.warn("Can't stop polling thread", e);
                }
            }
            return null;
        });
    }

    private Thread pollingThread;

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String eventTopic = environment.resolve(config.getString("event_topic"));

            final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

            final KafkaConsumer<String, String> kafkaConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext> eventHandler = new AccessHandler(jwtProvider, NotificationDispatcher.create(vertx), onAccessDenied, asList(ANONYMOUS, ADMIN, GUEST));

            final Map<String, BlockingHandler<InputMessage>> messageHandlers = new HashMap<>();

            messageHandlers.put(MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED, Factory.createDesignAggregateUpdateCompletedHandler(new DesignAggregateUpdateCompletedController(vertx, "notifications")));

            messageHandlers.put(MessageType.TILE_AGGREGATE_UPDATE_COMPLETED, Factory.createTileAggregateUpdateCompletedHandler(new TileAggregateUpdateCompletedController(vertx, "notifications")));

            final KafkaPolling kafkaPolling = new KafkaPolling();

            kafkaConsumer.subscribe(eventTopic);

            pollingThread = new Thread(() -> kafkaPolling.pollRecords(kafkaConsumer, messageHandlers), "kafka-records-poll");

            pollingThread.start();

            final Handler<RoutingContext> openapiHandler = new OpenApiHandler(vertx.getDelegate(), executor, "openapi.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("openapi.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource openapi.yaml");
            }

            final String url = resource.toURI().toString();

            RouterBuilder.create(vertx.getDelegate(), url)
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("watchDesign")
                                .handler(context -> eventHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("watchDesigns")
                                .handler(context -> eventHandler.handle(RoutingContext.newInstance(context)));
//
                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.route().handler(LoggerHandler.create());
                        mainRouter.route().handler(BodyHandler.create());
                        //mainRouter.route().handler(CookieHandler.create());

                        mainRouter.route("/*").handler(corsHandler);

                        mainRouter.mountSubRouter("/v1", apiRouter);

                        mainRouter.get("/v1/apidocs").handler(openapiHandler);

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
