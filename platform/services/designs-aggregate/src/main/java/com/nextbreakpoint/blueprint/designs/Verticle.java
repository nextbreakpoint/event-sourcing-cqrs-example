package com.nextbreakpoint.blueprint.designs;

import com.fasterxml.jackson.core.JsonParser;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.healthchecks.HealthCheckHandler;
import io.vertx.rxjava.ext.healthchecks.HealthChecks;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Completable;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.*;
import static java.util.Arrays.asList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    private KafkaPolling kafkaPolling1;
    private KafkaPolling kafkaPolling2;
    private KafkaPolling kafkaPolling3;

    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            DatabindCodec.mapper().configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

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
            if (kafkaPolling1 != null) {
                kafkaPolling1.stopPolling();
            }
            if (kafkaPolling2 != null) {
                kafkaPolling2.stopPolling();
            }
            if (kafkaPolling3 != null) {
                kafkaPolling3.stopPolling();
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

            final String batchTopic = config.getString("batch_topic");

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

            final String clientId = config.getString("kafka_client_id", "designs-aggregate");

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

            final KafkaConsumer<String, String> kafkaConsumer1 = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-1").build());
            final KafkaConsumer<String, String> kafkaConsumer2 = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-2").build());
            final KafkaConsumer<String, String> kafkaConsumer3 = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-3").build());
            final KafkaConsumer<String, String> kafkaConsumer4 = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-4").build());

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

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers1 = new HashMap<>();

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers2 = new HashMap<>();

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers3 = new HashMap<>();

            kafkaConsumer1.subscribe(eventsTopic);

            kafkaConsumer2.subscribe(eventsTopic);

            kafkaConsumer3.subscribe(batchTopic);

            messageHandlers1.put(DesignInsertRequested.TYPE, createDesignInsertRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignUpdateRequested.TYPE, createDesignUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignDeleteRequested.TYPE, createDesignDeleteRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers1.put(DesignAggregateUpdateRequested.TYPE, createDesignAggregateUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignAggregateUpdateCompleted.TYPE, createDesignAggregateUpdateCompletedHandler(store, eventsTopic, batchTopic, kafkaProducer, messageSource));
            messageHandlers1.put(DesignAggregateUpdateCancelled.TYPE, createDesignAggregateUpdateCancelledHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers1.put(TileAggregateUpdateRequested.TYPE, createTileAggregateUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers1.put(TileAggregateUpdateCompleted.TYPE, createTileAggregateUpdateCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers1.put(TileRenderCompleted.TYPE, createTileRenderCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers2.put(TileAggregateUpdateRequired.TYPE, createTileAggregateUpdateRequiredHandler(store, eventsTopic, kafkaProducer, messageSource));

            messageHandlers3.put(TilesRenderRequired.TYPE, createTilesRenderRequiredHandler(store, renderTopic, kafkaProducer, messageSource));

            kafkaPolling1 = new KafkaPolling(kafkaConsumer1, messageHandlers1, KafkaRecordsQueue.Simple.create(), -1, 50);

            kafkaPolling2 = new KafkaPolling(kafkaConsumer2, messageHandlers2, KafkaRecordsQueue.Compacted.create(), 5000, 50);

            kafkaPolling3 = new KafkaPolling(kafkaConsumer3, messageHandlers3, KafkaRecordsQueue.Simple.create(), 2000, 1);

            kafkaPolling1.startPolling("kafka-polling-topic-" + eventsTopic + "-1");

            kafkaPolling2.startPolling("kafka-polling-topic-" + eventsTopic + "-2");

            kafkaPolling3.startPolling("kafka-polling-topic-" + batchTopic);

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            healthCheckHandler.register("kafka-events-topic", future -> checkTopic(kafkaConsumer4, eventsTopic, future));
            healthCheckHandler.register("kafka-render-topic", future -> checkTopic(kafkaConsumer4, renderTopic, future));
            healthCheckHandler.register("kafka-batch-topic", future -> checkTopic(kafkaConsumer4, batchTopic, future));
            healthCheckHandler.register("cassandra-design-table", future -> checkTable(store, future, "DESIGN"));
            healthCheckHandler.register("cassandra-message-table", future -> checkTable(store, future, "MESSAGE"));

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
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

                        mainRouter.get("/health*").handler(healthCheckHandler);

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

    private void checkTable(Store store, Promise<Status> promise, String tableName) {
        store.existsTable(tableName)
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(exists -> promise.complete(exists ? Status.OK() : Status.KO()), err -> promise.complete(Status.KO()));
    }

    private void checkTopic(KafkaConsumer<String, String> kafkaConsumer, String eventsTopic, Promise<Status> promise) {
        kafkaConsumer.rxPartitionsFor(eventsTopic)
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
