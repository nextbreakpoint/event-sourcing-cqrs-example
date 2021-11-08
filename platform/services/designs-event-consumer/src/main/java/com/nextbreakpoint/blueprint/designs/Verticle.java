package com.nextbreakpoint.blueprint.designs;

import com.fasterxml.jackson.core.JsonParser;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;
import rx.plugins.RxJavaHooks;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.*;
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
                    .setTracingOptions(tracingOptions)
                    .setWorkerPoolSize(20);

            final Vertx vertx = Vertx.vertx(vertxOptions);

            RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
            RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
            RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

            DatabindCodec.mapper().configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

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
            if (pollingWihCompactionThread != null) {
                try {
                    pollingWihCompactionThread.interrupt();
                    pollingWihCompactionThread.join();
                } catch (InterruptedException e) {
                    logger.warn("Can't stop polling thread with compaction", e);
                }
            }
            return null;
        });
    }

    private Thread pollingThread;
    private Thread pollingWihCompactionThread;

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String renderingTopic = environment.resolve(config.getString("rendering_topic"));

            final String messageTopic = environment.resolve(config.getString("message_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final KafkaProducer<String, String> kafkaProducer = KafkaClientFactory.createProducer(environment, vertx, config);

            final JsonObject consumerConfig1 = new JsonObject(config.getMap());
            consumerConfig1.put("kafka_group_id", config.getValue("kafka_group_id", "test") + "-1");
            final KafkaConsumer<String, String> kafkaConsumer1 = KafkaClientFactory.createConsumer(environment, vertx, consumerConfig1);

            final JsonObject consumerConfig2 = new JsonObject(config.getMap());
            consumerConfig2.put("kafka_group_id", config.getValue("kafka_group_id", "test") + "-2");
            final KafkaConsumer<String, String> kafkaConsumer2 = KafkaClientFactory.createConsumer(environment, vertx, consumerConfig2);

            final Supplier<CassandraClient> supplier = () -> CassandraClientFactory.create(environment, vertx, config);

            final Store store = new CassandraStore(supplier);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(CONTENT_TYPE, X_XSRF_TOKEN));

            final Map<String, MessageHandler<Message, Void>> messageHandlers = new HashMap<>();

            final Map<String, MessageHandler<Message, Void>> messageWithCompactionHandlers = new HashMap<>();

            final KafkaPolling kafkaPolling = new KafkaPolling();

            kafkaConsumer1.subscribe(messageTopic);

            kafkaConsumer2.subscribe(messageTopic);

            messageHandlers.put(MessageType.DESIGN_INSERT_REQUESTED, createDesignInsertRequestedHandler(store, messageTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.DESIGN_UPDATE_REQUESTED, createDesignUpdateRequestedHandler(store, messageTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.DESIGN_DELETE_REQUESTED, createDesignDeleteRequestedHandler(store, messageTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.DESIGN_ABORT_REQUESTED, createDesignAbortRequestedHandler(store, messageTopic, kafkaProducer, messageSource));

            messageHandlers.put(MessageType.DESIGN_AGGREGATE_UPDATE_REQUESTED, createDesignAggregateUpdateRequestedHandler(store, messageTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.DESIGN_AGGREGATE_UPDATE_COMPLETED, createDesignAggregateUpdateCompletedHandler(store, messageTopic, kafkaProducer, messageSource));

            messageHandlers.put(MessageType.TILE_AGGREGATE_UPDATE_REQUESTED, createTileAggregateUpdateRequestedHandler(store, messageTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.TILE_AGGREGATE_UPDATE_COMPLETED, createTileAggregateUpdateCompletedHandler(store, messageTopic, kafkaProducer, messageSource));

            messageHandlers.put(MessageType.TILE_RENDER_REQUESTED, createTileRenderRequestedHandler(store, renderingTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.TILE_RENDER_COMPLETED, createTileRenderCompletedHandler(store, messageTopic, kafkaProducer, messageSource));
            messageHandlers.put(MessageType.TILE_RENDER_ABORTED, createTileRenderAbortedHandler(store, renderingTopic, kafkaProducer, messageSource));

            messageWithCompactionHandlers.put(MessageType.TILE_AGGREGATE_UPDATE_REQUIRED, createTileAggregateUpdateRequiredHandler(store, messageTopic, kafkaProducer, messageSource));

            pollingThread = new Thread(() -> kafkaPolling.pollRecords(kafkaConsumer1, messageHandlers), "kafka-records-poll");

            pollingThread.start();

            pollingWihCompactionThread = new Thread(() -> kafkaPolling.pollRecordsWithCompaction(kafkaConsumer2, messageWithCompactionHandlers), "kafka-records-poll-with-compaction");

            pollingWihCompactionThread.start();

            final Handler<RoutingContext> openapiHandler = new OpenApiHandler(vertx.getDelegate(), executor, "openapi.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("openapi.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource openapi.yaml");
            }

            final String url = resource.toURI().toString();

            RouterBuilder.create(vertx.getDelegate(), url)
                    .onSuccess(routerBuilder -> {
                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.route().handler(MDCHandler.create());
                        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
                        mainRouter.route().handler(BodyHandler.create());
                        //mainRouter.route().handler(CookieHandler.create());
                        mainRouter.route().handler(TimeoutHandler.create(30000));

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
