package com.nextbreakpoint.blueprint.designs;

import com.fasterxml.jackson.core.JsonParser;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.*;
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

    private KafkaPolling kafkaPolling1;
    private KafkaPolling kafkaPolling2;

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
            if (kafkaPolling1 != null) {
                kafkaPolling1.stopPolling();
            }
            if (kafkaPolling2 != null) {
                kafkaPolling2.stopPolling();
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

            final String renderTopic = config.getString("render_topic");

            final String eventsTopic = config.getString("events_topic");

            final String messageSource = config.getString("message_source");

            final String clusterName = config.getString("cassandra_cluster");

            final String keyspace = config.getString("cassandra_keyspace");

            final String username = config.getString("cassandra_username");

            final String password = config.getString("cassandra_password");

            final String[] contactPoints = config.getString("cassandra_contactPoints").split(",");

            final int cassandraPort = Integer.parseInt(config.getString("cassandra_port", "9042"));

            final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");

            final String keySerializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringSerializer");

            final String valSerializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringSerializer");

            final String clientId = config.getString("kafka_client_id", "designs-event-consumer");

            final String acks = config.getString("kafka_acks", "1");

            final String keystoreLocation = config.getString("kafka_keystore_location");

            final String keystorePassword = config.getString("kafka_keystore_password");

            final String truststoreLocation = config.getString("kafka_truststore_location");

            final String truststorePassword = config.getString("kafka_truststore_password");

            final String keyDeserializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringDeserializer");

            final String valDeserializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringDeserializer");

            final String groupId = config.getString("kafka_group_id", "test");

            final String autoOffsetReset = config.getString("kafka_auto_offset_reset", "earliest");

            final String enableAutoCommit = config.getString("kafka_enable_auto_commit", "false");

            final KafkaProducerConfig producerConfig = KafkaProducerConfig.builder()
                    .withBootstrapServers(bootstrapServers)
                    .withKeySerializer(keySerializer)
                    .withValueSerializer(valSerializer)
                    .withKeystoreLocation(keystoreLocation)
                    .withKeystorePassword(keystorePassword)
                    .withTruststoreLocation(truststoreLocation)
                    .withTruststorePassword(truststorePassword)
                    .withClientId(clientId)
                    .withKafkaAcks(acks)
                    .build();

            final KafkaProducer<String, String> kafkaProducer = KafkaClientFactory.createProducer(vertx, producerConfig);

            final KafkaConsumerConfig consumerConfig1 = KafkaConsumerConfig.builder()
                    .withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(keyDeserializer)
                    .withValueDeserializer(valDeserializer)
                    .withKeystoreLocation(keystoreLocation)
                    .withKeystorePassword(keystorePassword)
                    .withTruststoreLocation(truststoreLocation)
                    .withTruststorePassword(truststorePassword)
                    .withGroupId(groupId + "-1")
                    .withAutoOffsetReset(autoOffsetReset)
                    .withEnableAutoCommit(enableAutoCommit)
                    .build();

            final KafkaConsumerConfig consumerConfig2 = KafkaConsumerConfig.builder()
                    .withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(keyDeserializer)
                    .withValueDeserializer(valDeserializer)
                    .withKeystoreLocation(keystoreLocation)
                    .withKeystorePassword(keystorePassword)
                    .withTruststoreLocation(truststoreLocation)
                    .withTruststorePassword(truststorePassword)
                    .withGroupId(groupId + "-2")
                    .withAutoOffsetReset(autoOffsetReset)
                    .withEnableAutoCommit(enableAutoCommit)
                    .build();

            final KafkaConsumer<String, String> kafkaConsumer1 = KafkaClientFactory.createConsumer(vertx, consumerConfig1);

            final KafkaConsumer<String, String> kafkaConsumer2 = KafkaClientFactory.createConsumer(vertx, consumerConfig2);

            final CassandraClientConfig cassandraConfig = CassandraClientConfig.builder()
                    .withClusterName(clusterName)
                    .withKeyspace(keyspace)
                    .withUsername(username)
                    .withPassword(password)
                    .withContactPoints(contactPoints)
                    .withPort(cassandraPort)
                    .build();

            final Supplier<CassandraClient> supplier = () -> CassandraClientFactory.create(vertx, cassandraConfig);

            final Store store = new CassandraStore(keyspace, supplier);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(CONTENT_TYPE, X_XSRF_TOKEN));

            final Map<String, BlockingHandler<InputMessage>> messageHandlers1 = new HashMap<>();

            final Map<String, BlockingHandler<InputMessage>> messageHandlers2 = new HashMap<>();

            kafkaConsumer1.subscribe(eventsTopic);

            kafkaConsumer2.subscribe(eventsTopic);

            messageHandlers1.put(DesignInsertRequested.TYPE, createDesignInsertRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignUpdateRequested.TYPE, createDesignUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignDeleteRequested.TYPE, createDesignDeleteRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignAbortRequested.TYPE, createDesignAbortRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers1.put(DesignAggregateUpdateRequested.TYPE, createDesignAggregateUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignAggregateUpdateCompleted.TYPE, createDesignAggregateUpdateCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers1.put(TileAggregateUpdateRequested.TYPE, createTileAggregateUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(TileAggregateUpdateCompleted.TYPE, createTileAggregateUpdateCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers1.put(TileRenderRequested.TYPE, createTileRenderRequestedHandler(store, renderTopic, kafkaProducer, messageSource));
            messageHandlers1.put(TileRenderCompleted.TYPE, createTileRenderCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(TileRenderAborted.TYPE, createTileRenderAbortedHandler(store, renderTopic, kafkaProducer, messageSource));

            messageHandlers2.put(TileAggregateUpdateRequired.TYPE, createTileAggregateUpdateRequiredHandler(store, eventsTopic, kafkaProducer, messageSource));

            kafkaPolling1 = new KafkaPolling(kafkaConsumer1, messageHandlers1);

            kafkaPolling2 = new KafkaPolling(kafkaConsumer2, messageHandlers2, KafkaRecordsQueue.Compacted, 30000);

            kafkaPolling1.startPolling("kafka-records-poll-1");

            kafkaPolling2.startPolling("kafka-records-poll-2");

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
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
