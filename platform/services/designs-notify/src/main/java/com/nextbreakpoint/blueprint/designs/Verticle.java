package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.handlers.NotificationHandler;
import com.nextbreakpoint.blueprint.designs.controllers.DesignAggregateUpdateCompletedController;
import com.nextbreakpoint.blueprint.designs.controllers.TileAggregateUpdateCompletedController;
import com.nextbreakpoint.blueprint.designs.handlers.WatchHandler;
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
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
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

    private KafkaPolling kafkaPolling;

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

    @Override
    public Completable rxStop() {
        return Completable.fromCallable(() -> {
            if (kafkaPolling != null) {
                kafkaPolling.stopPolling();
            }
            return null;
        });
    }

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(config.getString("host_port"));

            final String originPattern = config.getString("origin_pattern");

            final String jksStorePath = config.getString("server_keystore_path");

            final String jksStoreSecret = config.getString("server_keystore_secret");

            final String eventsTopic = config.getString("events_topic");

            final String jwtKeystoreType = config.getString("jwt_keystore_type");

            final String jwtKeystorePath = config.getString("jwt_keystore_path");

            final String jwtKeystoreSecret = config.getString("jwt_keystore_secret");

            final String consulHost = config.getString("consul_host");

            final Integer consulPort = Integer.parseInt(config.getString("consul_port"));

            final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");

            final String keyDeserializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringDeserializer");

            final String valDeserializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringDeserializer");

            final String groupId = config.getString("kafka_group_id", "test");

            final String autoOffsetReset = config.getString("kafka_auto_offset_reset", "earliest");

            final String enableAutoCommit = config.getString("kafka_enable_auto_commit", "false");

            final String keystoreLocation = config.getString("kafka_keystore_location");

            final String keystorePassword = config.getString("kafka_keystore_password");

            final String truststoreLocation = config.getString("kafka_truststore_location");

            final String truststorePassword = config.getString("kafka_truststore_password");

            final JWTProviderConfig jwtProviderConfig = JWTProviderConfig.builder()
                    .withKeyStoreType(jwtKeystoreType)
                    .withKeyStorePath(jwtKeystorePath)
                    .withKeyStoreSecret(jwtKeystoreSecret)
                    .build();

            final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, jwtProviderConfig);

            final KafkaConsumerConfig consumerConfig = KafkaConsumerConfig.builder()
                    .withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(keyDeserializer)
                    .withValueDeserializer(valDeserializer)
                    .withKeystoreLocation(keystoreLocation)
                    .withKeystorePassword(keystorePassword)
                    .withTruststoreLocation(truststoreLocation)
                    .withTruststorePassword(truststorePassword)
                    .withGroupId(groupId)
                    .withAutoOffsetReset(autoOffsetReset)
                    .withEnableAutoCommit(enableAutoCommit)
                    .build();

            final KafkaConsumer<String, String> kafkaConsumer = KafkaClientFactory.createConsumer(vertx, consumerConfig);

            final Router mainRouter = Router.router(vertx);

            final JsonObject configuration = new JsonObject()
                    .put("host", consulHost)
                    .put("port", consulPort)
                    .put("scan-period", 2000);

            final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx);

            serviceDiscovery.registerServiceImporter(new ServiceImporter(new ConsulServiceImporter()), configuration);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext> watchHandler = new AccessHandler(jwtProvider, new WatchHandler(serviceDiscovery), onAccessDenied, asList(ANONYMOUS, ADMIN, GUEST));

            final Handler<RoutingContext> sseHandler = new AccessHandler(jwtProvider, NotificationHandler.create(vertx), onAccessDenied, asList(ANONYMOUS, ADMIN, GUEST));

            final Map<String, BlockingHandler<InputMessage>> messageHandlers = new HashMap<>();

            messageHandlers.put(DesignAggregateUpdateCompleted.TYPE, Factory.createDesignAggregateUpdateCompletedHandler(new DesignAggregateUpdateCompletedController(vertx, "notifications")));

            messageHandlers.put(TileAggregateUpdateCompleted.TYPE, Factory.createTileAggregateUpdateCompletedHandler(new TileAggregateUpdateCompletedController(vertx, "notifications")));

            kafkaConsumer.subscribe(eventsTopic);

            kafkaPolling = new KafkaPolling(kafkaConsumer, messageHandlers);

            kafkaPolling.startPolling("kafka-records-poll");

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final String url = resource.toURI().toString();

            RouterBuilder.create(vertx.getDelegate(), url)
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("watchDesign")
                                .handler(context -> watchHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("watchDesigns")
                                .handler(context -> watchHandler.handle(RoutingContext.newInstance(context)));
//
                        routerBuilder.operation("sseDesign")
                                .handler(context -> sseHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("sseDesigns")
                                .handler(context -> sseHandler.handle(RoutingContext.newInstance(context)));
//
                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.route().handler(LoggerHandler.create());
                        mainRouter.route().handler(BodyHandler.create());
                        //mainRouter.route().handler(CookieHandler.create());

                        mainRouter.route("/*").handler(corsHandler);

                        mainRouter.mountSubRouter("/v1", apiRouter);

                        mainRouter.get("/v1/apidocs").handler(apiV1DocsHandler);

                        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

                        final ServerConfig serverConfig = ServerConfig.builder()
                                .withJksStorePath(jksStorePath)
                                .withJksStoreSecret(jksStoreSecret)
                                .build();

                        final HttpServerOptions options = Server.makeOptions(serverConfig);

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
