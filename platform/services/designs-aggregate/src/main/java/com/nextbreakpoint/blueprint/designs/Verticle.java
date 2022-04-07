package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.core.JsonParser;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.CassandraClientConfig;
import com.nextbreakpoint.blueprint.common.drivers.CassandraClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.drivers.*;
import com.nextbreakpoint.blueprint.common.events.*;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
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
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import rx.Completable;
import rx.Single;

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
import java.util.function.Supplier;

import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.*;

@Log4j2
public class Verticle extends AbstractVerticle {
    private KafkaPolling eventsKafkaPolling;
    private KafkaPolling cancelKafkaPolling;
    private KafkaPolling bufferKafkaPolling;
    private KafkaPolling renderKafkaPolling0;
    private KafkaPolling renderKafkaPolling1;
    private KafkaPolling renderKafkaPolling2;
    private KafkaPolling renderKafkaPolling3;

    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            DatabindCodec.mapper().configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

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

    @Override
    public Completable rxStop() {
        return Completable.fromCallable(() -> {
            if (eventsKafkaPolling != null) {
                eventsKafkaPolling.stopPolling();
            }
            if (cancelKafkaPolling != null) {
                cancelKafkaPolling.stopPolling();
            }
            if (bufferKafkaPolling != null) {
                bufferKafkaPolling.stopPolling();
            }
            if (renderKafkaPolling0 != null) {
                renderKafkaPolling0.stopPolling();
            }
            if (renderKafkaPolling1 != null) {
                renderKafkaPolling1.stopPolling();
            }
            if (renderKafkaPolling2 != null) {
                renderKafkaPolling2.stopPolling();
            }
            if (renderKafkaPolling3 != null) {
                renderKafkaPolling3.stopPolling();
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

            final String renderTopicPrefix = config.getString("render_topic_prefix");

            final String eventsTopic = config.getString("events_topic");

            final String cancelTopic = config.getString("cancel_topic");

            final String bufferTopic = config.getString("buffer_topic");

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

            final KafkaProducer<String, String> kafkaProducer = KafkaClientFactory.createProducer(producerConfig);

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

            final KafkaConsumer<String, String> healthKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-health").build());
            final KafkaConsumer<String, String> eventsKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-events").build());
            final KafkaConsumer<String, String> cancelKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-cancel").build());
            final KafkaConsumer<String, String> bufferKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-buffer").build());
            final KafkaConsumer<String, String> renderKafkaConsumer0 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-0").build());
            final KafkaConsumer<String, String> renderKafkaConsumer1 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-1").build());
            final KafkaConsumer<String, String> renderKafkaConsumer2 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-2").build());
            final KafkaConsumer<String, String> renderKafkaConsumer3 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-3").build());

            final CassandraClientConfig cassandraConfig = CassandraClientConfig.builder()
                    .withClusterName(clusterName)
                    .withKeyspace(keyspace)
                    .withUsername(username)
                    .withPassword(password)
                    .withContactPoints(contactPoints)
                    .withPort(cassandraPort)
                    .build();

            final Supplier<CqlSession> supplier = () -> CassandraClientFactory.create(cassandraConfig);

            final Store store = new CassandraStore(supplier);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, List.of(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), List.of(CONTENT_TYPE, X_XSRF_TOKEN));

            final Map<String, RxSingleHandler<InputMessage, ?>> eventsMessageHandlers = new HashMap<>();

            final Map<String, RxSingleHandler<InputMessage, ?>> cancelMessageHandlers = new HashMap<>();

            final Map<String, RxSingleHandler<InputMessage, ?>> renderMessageHandlers = new HashMap<>();

            final Map<String, RxSingleHandler<List<InputMessage>, ?>> bufferMessageHandlers = new HashMap<>();

            eventsKafkaConsumer.subscribe(List.of(eventsTopic));

            cancelKafkaConsumer.subscribe(List.of(cancelTopic));

            bufferKafkaConsumer.subscribe(List.of(bufferTopic));

            renderKafkaConsumer0.subscribe(List.of(renderTopicPrefix + "-completed-0"));
            renderKafkaConsumer1.subscribe(List.of(renderTopicPrefix + "-completed-1"));
            renderKafkaConsumer2.subscribe(List.of(renderTopicPrefix + "-completed-2"));
            renderKafkaConsumer3.subscribe(List.of(renderTopicPrefix + "-completed-3"));

            eventsMessageHandlers.put(DesignInsertRequested.TYPE, createDesignInsertRequestedHandler(store, eventsTopic, cancelTopic, renderTopicPrefix, kafkaProducer, messageSource));
            eventsMessageHandlers.put(DesignUpdateRequested.TYPE, createDesignUpdateRequestedHandler(store, eventsTopic, cancelTopic, renderTopicPrefix, kafkaProducer, messageSource));
            eventsMessageHandlers.put(DesignDeleteRequested.TYPE, createDesignDeleteRequestedHandler(store, eventsTopic, cancelTopic, renderTopicPrefix, kafkaProducer, messageSource));

            eventsMessageHandlers.put(DesignAggregateUpdated.TYPE, createDesignAggregateTilesUpdateCompletedHandler(eventsTopic, kafkaProducer, messageSource));

            eventsMessageHandlers.put(TilesRendered.TYPE, createTilesRenderedHandler(store, eventsTopic, renderTopicPrefix, kafkaProducer, messageSource));

            cancelMessageHandlers.put(TileRenderCancelled.TYPE, createTileRenderCancelledHandler(cancelTopic, renderTopicPrefix, kafkaProducer, messageSource));

            bufferMessageHandlers.put(TileRenderCompleted.TYPE, createTileRenderCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));

            renderMessageHandlers.put(TileRenderCompleted.TYPE, createForwardTileRenderCompletedHandler(bufferTopic, kafkaProducer, messageSource));

            eventsKafkaPolling = new KafkaPolling<>(eventsKafkaConsumer, eventsMessageHandlers, KafkaRecordsConsumer.Simple.create(eventsMessageHandlers), KafkaRecordsQueue.Simple.create(), -1, 20);

            cancelKafkaPolling = new KafkaPolling<>(cancelKafkaConsumer, cancelMessageHandlers, KafkaRecordsConsumer.Simple.create(cancelMessageHandlers), KafkaRecordsQueue.Simple.create(), -1, 20);

            bufferKafkaPolling = new KafkaPolling<>(bufferKafkaConsumer, bufferMessageHandlers, KafkaRecordsConsumer.Buffered.create(bufferMessageHandlers), KafkaRecordsQueue.Simple.create(), 2500, 100);

            renderKafkaPolling0 = new KafkaPolling<>(renderKafkaConsumer0, renderMessageHandlers, KafkaRecordsConsumer.Simple.create(renderMessageHandlers), KafkaRecordsQueue.Compacted.create(), -1, 50);
            renderKafkaPolling1 = new KafkaPolling<>(renderKafkaConsumer1, renderMessageHandlers, KafkaRecordsConsumer.Simple.create(renderMessageHandlers), KafkaRecordsQueue.Compacted.create(), -1, 50);
            renderKafkaPolling2 = new KafkaPolling<>(renderKafkaConsumer2, renderMessageHandlers, KafkaRecordsConsumer.Simple.create(renderMessageHandlers), KafkaRecordsQueue.Compacted.create(), -1, 50);
            renderKafkaPolling3 = new KafkaPolling<>(renderKafkaConsumer3, renderMessageHandlers, KafkaRecordsConsumer.Simple.create(renderMessageHandlers), KafkaRecordsQueue.Compacted.create(), -1, 50);

            eventsKafkaPolling.startPolling("kafka-polling-topic-" + eventsTopic);

            cancelKafkaPolling.startPolling("kafka-polling-topic-" + cancelTopic);

            bufferKafkaPolling.startPolling("kafka-polling-topic-" + bufferTopic);

            renderKafkaPolling0.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-completed-0");
            renderKafkaPolling1.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-completed-1");
            renderKafkaPolling2.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-completed-2");
            renderKafkaPolling3.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-completed-3");

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            healthCheckHandler.register("kafka-topic-events", 2000, future -> checkTopic(healthKafkaConsumer, eventsTopic, future));
            healthCheckHandler.register("kafka-topic-cancel", 2000, future -> checkTopic(healthKafkaConsumer, cancelTopic, future));
            healthCheckHandler.register("kafka-topic-buffer", 2000, future -> checkTopic(healthKafkaConsumer, bufferTopic, future));
            healthCheckHandler.register("kafka-topic-render-completed-0", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-0", future));
            healthCheckHandler.register("kafka-topic-render-completed-1", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-1", future));
            healthCheckHandler.register("kafka-topic-render-completed-2", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-2", future));
            healthCheckHandler.register("kafka-topic-render-completed-3", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-3", future));
            healthCheckHandler.register("kafka-topic-render-requested-0", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-0", future));
            healthCheckHandler.register("kafka-topic-render-requested-1", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-1", future));
            healthCheckHandler.register("kafka-topic-render-requested-2", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-2", future));
            healthCheckHandler.register("kafka-topic-render-requested-3", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-3", future));
            healthCheckHandler.register("cassandra-table-design", 2000, future -> checkTable(store, future, "DESIGN"));
            healthCheckHandler.register("cassandra-table-message", 2000, future -> checkTable(store, future, "MESSAGE"));

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
                    .onSuccess(routerBuilder -> {
                        final Router apiRouter = Router.newInstance(routerBuilder.createRouter());

                        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
                        mainRouter.route().handler(BodyHandler.create());
                        mainRouter.route().handler(TimeoutHandler.create(30000));

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

    private void checkTable(Store store, Promise<Status> promise, String tableName) {
        store.existsTable(tableName)
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(exists -> promise.complete(exists ? Status.OK() : Status.KO()), err -> promise.complete(Status.KO()));
    }

    private void checkTopic(KafkaConsumer<String, String> kafkaConsumer, String eventsTopic, Promise<Status> promise) {
        Single.fromCallable(() -> kafkaConsumer.partitionsFor(eventsTopic))
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
