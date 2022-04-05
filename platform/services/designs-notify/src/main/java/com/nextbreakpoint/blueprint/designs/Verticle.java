package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteCompleted;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateCompleted;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.controllers.DesignDocumentDeleteCompletedController;
import com.nextbreakpoint.blueprint.designs.controllers.DesignDocumentUpdateCompletedController;
import com.nextbreakpoint.blueprint.designs.handlers.NotificationHandler;
import com.nextbreakpoint.blueprint.designs.handlers.WatchHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.healthchecks.HealthCheckHandler;
import io.vertx.rxjava.ext.healthchecks.HealthChecks;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import rx.Completable;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Authority.*;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    private KafkaPolling kafkaPolling;

    private boolean discoveryStarted;

    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            vertx.rxDeployVerticle(new Verticle(), new DeploymentOptions().setConfig(config))
                    .delay(30, TimeUnit.SECONDS)
                    .retry(3)
                    .subscribe(o -> logger.info("Verticle deployed"), err -> logger.error("Can't deploy verticle"));
        } catch (Exception e) {
            logger.error("Can't start service", e);
        }
    }

    @Override
    public Completable rxStart() {
        return vertx.rxExecuteBlocking(this::initServer).toCompletable();
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
                    .withAutoOffsetReset(autoOffsetReset)
                    .withEnableAutoCommit(enableAutoCommit)
                    .build();

            final KafkaConsumer<String, String> eventsKafkaConsumer = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-events").build());
            final KafkaConsumer<String, String> healthKafkaConsumer = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-health").build());

            final Router mainRouter = Router.router(vertx);

            final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext> watchHandler = new AccessHandler(jwtProvider, new WatchHandler(serviceDiscovery), onAccessDenied, asList(ANONYMOUS, ADMIN, GUEST));

            final Handler<RoutingContext> sseHandler = new AccessHandler(jwtProvider, NotificationHandler.create(vertx), onAccessDenied, asList(ANONYMOUS, ADMIN, GUEST));

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers = new HashMap<>();

            messageHandlers.put(DesignDocumentUpdateCompleted.TYPE, Factory.createDesignDocumentUpdateCompletedHandler(new DesignDocumentUpdateCompletedController(vertx, "notifications")));
            messageHandlers.put(DesignDocumentDeleteCompleted.TYPE, Factory.createDesignDocumentDeleteCompletedHandler(new DesignDocumentDeleteCompletedController(vertx, "notifications")));

            eventsKafkaConsumer.subscribe(eventsTopic);

            kafkaPolling = new KafkaPolling<>(eventsKafkaConsumer, messageHandlers, KafkaRecordsConsumer.Simple.create(messageHandlers));

            kafkaPolling.startPolling("kafka-polling-topic-" + eventsTopic);

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            healthCheckHandler.register("kafka-topic-events", 2000, future -> checkTopic(healthKafkaConsumer, eventsTopic, future));
            healthCheckHandler.register("service-discovery-importer", 2000, future -> checkDiscoveryImporter(serviceDiscovery, future));
            healthCheckHandler.register("service-discovery-records", 2000, future -> checkDiscoveryRecords(serviceDiscovery, future));

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            final JsonObject scanConfig = new JsonObject()
                    .put("host", consulHost)
                    .put("port", consulPort)
                    .put("scan-period", 5000);

            startDiscovery(serviceDiscovery, scanConfig);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
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

                        mainRouter.get("/health*").handler(healthCheckHandler);

                        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                        mainRouter.route("/metrics").handler(PrometheusScrapingHandler.create());

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

    private void startDiscovery(ServiceDiscovery serviceDiscovery, JsonObject config) {
        serviceDiscovery.registerServiceImporter(new ServiceImporter(new ConsulServiceImporter()), config, result -> {
            discoveryStarted = result.succeeded();

            if (result.succeeded()) {
                logger.info("Service importer registered", result.cause());
            } else {
                logger.error("Can't register service importer", result.cause());
            }
        });
    }

    private void checkDiscoveryImporter(ServiceDiscovery serviceDiscovery, Promise<Status> promise) {
        if (discoveryStarted) {
            promise.complete();
        } else {
            promise.fail("Service importer not started");
        }
    }

    private void checkDiscoveryRecords(ServiceDiscovery serviceDiscovery, Promise<Status> promise) {
        serviceDiscovery
                .rxGetRecords(record -> record.getName().equals("designs-sse"))
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(records -> promise.complete(records.isEmpty() ? Status.KO() : Status.OK()), err -> promise.complete(Status.KO()));
    }

    private void checkTopic(KafkaConsumer<String, String> kafkaConsumer, String eventsTopic, Promise<Status> promise) {
        kafkaConsumer.rxPartitionsFor(eventsTopic)
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
