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
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageConsumer;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessagePolling;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaRecordsQueue;
import com.nextbreakpoint.blueprint.common.events.avro.DesignAggregateUpdated;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignInsertRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderCompleted;
import com.nextbreakpoint.blueprint.common.events.avro.TilesRendered;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.Initializer;
import com.nextbreakpoint.blueprint.common.vertx.OpenApiHandler;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.Server;
import com.nextbreakpoint.blueprint.common.vertx.ServerConfig;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.backends.BackendRegistries;
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

import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.blueprint.designs.Factory.createBufferedTileRenderCompletedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignAggregateUpdatedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignDeleteRequestedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignInsertRequestedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignUpdateRequestedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createTileRenderCompletedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createTilesRenderedHandler;

@Log4j2
public class Verticle extends AbstractVerticle {
    private KafkaMessagePolling<Payload, Object, InputMessage<Object>> eventsKafkaPolling;
    private KafkaMessagePolling<Payload, Object, List<InputMessage<Object>>> bufferKafkaPolling;
    private KafkaMessagePolling<Payload, Object, InputMessage<Object>> renderKafkaPolling0;
    private KafkaMessagePolling<Payload, Object, InputMessage<Object>> renderKafkaPolling1;
    private KafkaMessagePolling<Payload, Object, InputMessage<Object>> renderKafkaPolling2;
    private KafkaMessagePolling<Payload, Object, InputMessage<Object>> renderKafkaPolling3;

    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

            DatabindCodec.mapper().configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

            vertx.rxDeployVerticle(new Verticle(), new DeploymentOptions().setConfig(config))
                    .delay(30, TimeUnit.SECONDS)
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

    @Override
    public Completable rxStop() {
        return Completable.fromCallable(() -> {
            if (eventsKafkaPolling != null) {
                eventsKafkaPolling.stopPolling();
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

            final String bufferTopic = config.getString("buffer_topic");

            final String messageSource = config.getString("message_source");

            final String clusterName = config.getString("cassandra_cluster");

            final String keyspace = config.getString("cassandra_keyspace");

            final String username = config.getString("cassandra_username");

            final String password = config.getString("cassandra_password");

            final String[] contactPoints = config.getString("cassandra_contactPoints").split(",");

            final int cassandraPort = Integer.parseInt(config.getString("cassandra_port", "9042"));

            final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");

            final String schemaRegistryUrl = config.getString("schema_registry_url", "http://localhost:8081");

            final String clientId = config.getString("kafka_client_id", "designs-aggregate");

            final String acks = config.getString("kafka_acks", "1");

            final String keystoreLocation = config.getString("kafka_keystore_location");

            final String keystorePassword = config.getString("kafka_keystore_password");

            final String truststoreLocation = config.getString("kafka_truststore_location");

            final String truststorePassword = config.getString("kafka_truststore_password");

            final String schemaRegistryKeystoreLocation = config.getString("schema_registry_keystore_location");

            final String schemaRegistryKeystorePassword = config.getString("schema_registry_keystore_password");

            final String schemaRegistryTruststoreLocation = config.getString("schema_registry_truststore_location");

            final String schemaRegistryTruststorePassword = config.getString("schema_registry_truststore_password");

            final String groupId = config.getString("kafka_group_id", "test");

            final String autoOffsetReset = config.getString("kafka_auto_offset_reset", "earliest");

            final String enableAutoCommit = config.getString("kafka_enable_auto_commit", "false");

            final String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
            final String valSerializer = "io.confluent.kafka.serializers.KafkaAvroSerializer";
            final String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
            final String valDeserializer = "io.confluent.kafka.serializers.KafkaAvroDeserializer";

            final MeterRegistry registry = BackendRegistries.getDefaultNow();

            final KafkaProducerConfig producerConfig = KafkaProducerConfig.builder()
                    .withBootstrapServers(bootstrapServers)
                    .withSchemaRegistryUrl(schemaRegistryUrl)
                    .withKeySerializer(keySerializer)
                    .withValueSerializer(valSerializer)
                    .withKeystoreLocation(keystoreLocation)
                    .withKeystorePassword(keystorePassword)
                    .withTruststoreLocation(truststoreLocation)
                    .withTruststorePassword(truststorePassword)
                    .withSchemaRegistryKeystoreLocation(schemaRegistryKeystoreLocation)
                    .withSchemaRegistryKeystorePassword(schemaRegistryKeystorePassword)
                    .withSchemaRegistryTruststoreLocation(schemaRegistryTruststoreLocation)
                    .withSchemaRegistryTruststorePassword(schemaRegistryTruststorePassword)
                    .withClientId(clientId)
                    .withKafkaAcks(acks)
                    .withAutoRegisterSchemas(true)
                    .build();

            final KafkaProducer<String, Payload> kafkaProducer = KafkaClientFactory.createProducer(producerConfig);

            final KafkaConsumerConfig consumerConfig = KafkaConsumerConfig.builder()
                    .withBootstrapServers(bootstrapServers)
                    .withSchemaRegistryUrl(schemaRegistryUrl)
                    .withKeyDeserializer(keyDeserializer)
                    .withValueDeserializer(valDeserializer)
                    .withKeystoreLocation(keystoreLocation)
                    .withKeystorePassword(keystorePassword)
                    .withTruststoreLocation(truststoreLocation)
                    .withTruststorePassword(truststorePassword)
                    .withSchemaRegistryKeystoreLocation(schemaRegistryKeystoreLocation)
                    .withSchemaRegistryKeystorePassword(schemaRegistryKeystorePassword)
                    .withSchemaRegistryTruststoreLocation(schemaRegistryTruststoreLocation)
                    .withSchemaRegistryTruststorePassword(schemaRegistryTruststorePassword)
                    .withAutoOffsetReset(autoOffsetReset)
                    .withEnableAutoCommit(enableAutoCommit)
                    .withAutoRegisterSchemas(false)
                    .withSpecificAvroReader(true)
                    .build();

            final KafkaConsumer<String, Object> healthKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-health").build());
            final KafkaConsumer<String, Payload> eventsKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-events").build());
            final KafkaConsumer<String, Payload> bufferKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-buffer").build());
            final KafkaConsumer<String, Payload> renderKafkaConsumer0 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-0").build());
            final KafkaConsumer<String, Payload> renderKafkaConsumer1 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-1").build());
            final KafkaConsumer<String, Payload> renderKafkaConsumer2 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-2").build());
            final KafkaConsumer<String, Payload> renderKafkaConsumer3 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-completed-3").build());

            final CassandraClientConfig cassandraConfig = CassandraClientConfig.builder()
                    .withClusterName(clusterName)
                    .withKeyspace(keyspace)
                    .withUsername(username)
                    .withPassword(password)
                    .withContactPoints(contactPoints)
                    .withPort(cassandraPort)
                    .build();

            final Supplier<CqlSession> supplier = () -> CassandraClientFactory.create(cassandraConfig, registry);

            final Store store = new CassandraStore(supplier);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, List.of(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), List.of(CONTENT_TYPE, X_XSRF_TOKEN));

            final Map<String, RxSingleHandler<InputMessage<Object>, Void>> eventsMessageHandlers = new HashMap<>();

            final Map<String, RxSingleHandler<InputMessage<Object>, Void>> renderMessageHandlers = new HashMap<>();

            final Map<String, RxSingleHandler<List<InputMessage<Object>>, Void>> bufferMessageHandlers = new HashMap<>();

            eventsKafkaConsumer.subscribe(List.of(eventsTopic));

            bufferKafkaConsumer.subscribe(List.of(bufferTopic));

            renderKafkaConsumer0.subscribe(List.of(renderTopicPrefix + "-completed-0"));
            renderKafkaConsumer1.subscribe(List.of(renderTopicPrefix + "-completed-1"));
            renderKafkaConsumer2.subscribe(List.of(renderTopicPrefix + "-completed-2"));
            renderKafkaConsumer3.subscribe(List.of(renderTopicPrefix + "-completed-3"));

            eventsMessageHandlers.put(DesignInsertRequested.getClassSchema().getFullName(), createDesignInsertRequestedHandler(store, eventsTopic, renderTopicPrefix, kafkaProducer, messageSource));
            eventsMessageHandlers.put(DesignUpdateRequested.getClassSchema().getFullName(), createDesignUpdateRequestedHandler(store, eventsTopic, renderTopicPrefix, kafkaProducer, messageSource));
            eventsMessageHandlers.put(DesignDeleteRequested.getClassSchema().getFullName(), createDesignDeleteRequestedHandler(store, eventsTopic, renderTopicPrefix, kafkaProducer, messageSource));

            eventsMessageHandlers.put(DesignAggregateUpdated.getClassSchema().getFullName(), createDesignAggregateUpdatedHandler(eventsTopic, kafkaProducer, messageSource));

            eventsMessageHandlers.put(TilesRendered.getClassSchema().getFullName(), createTilesRenderedHandler(store, eventsTopic, renderTopicPrefix, kafkaProducer, messageSource));

            bufferMessageHandlers.put(TileRenderCompleted.getClassSchema().getFullName(), createBufferedTileRenderCompletedHandler(store, eventsTopic, kafkaProducer, messageSource));

            renderMessageHandlers.put(TileRenderCompleted.getClassSchema().getFullName(), createTileRenderCompletedHandler(store, bufferTopic, renderTopicPrefix, kafkaProducer, messageSource));

            new KafkaClientMetrics(kafkaProducer).bindTo(registry);
            new KafkaClientMetrics(healthKafkaConsumer).bindTo(registry);
            new KafkaClientMetrics(eventsKafkaConsumer).bindTo(registry);
            new KafkaClientMetrics(bufferKafkaConsumer).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer0).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer1).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer2).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer3).bindTo(registry);

            eventsKafkaPolling = new KafkaMessagePolling<>(eventsKafkaConsumer, Records.createEventInputRecordMapper(), eventsMessageHandlers, KafkaMessageConsumer.Simple.create(eventsMessageHandlers, registry), registry, KafkaRecordsQueue.Simple.create(), -1, 20);

            bufferKafkaPolling = new KafkaMessagePolling<>(bufferKafkaConsumer, Records.createEventInputRecordMapper(), bufferMessageHandlers, KafkaMessageConsumer.Buffered.create(bufferMessageHandlers, registry), registry, KafkaRecordsQueue.Simple.create(), 2500, 100);

            renderKafkaPolling0 = new KafkaMessagePolling<>(renderKafkaConsumer0, Records.createEventInputRecordMapper(), renderMessageHandlers, KafkaMessageConsumer.Simple.create(renderMessageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);
            renderKafkaPolling1 = new KafkaMessagePolling<>(renderKafkaConsumer1, Records.createEventInputRecordMapper(), renderMessageHandlers, KafkaMessageConsumer.Simple.create(renderMessageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);
            renderKafkaPolling2 = new KafkaMessagePolling<>(renderKafkaConsumer2, Records.createEventInputRecordMapper(), renderMessageHandlers, KafkaMessageConsumer.Simple.create(renderMessageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);
            renderKafkaPolling3 = new KafkaMessagePolling<>(renderKafkaConsumer3, Records.createEventInputRecordMapper(), renderMessageHandlers, KafkaMessageConsumer.Simple.create(renderMessageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);

            eventsKafkaPolling.startPolling();

            bufferKafkaPolling.startPolling();

            renderKafkaPolling0.startPolling();
            renderKafkaPolling1.startPolling();
            renderKafkaPolling2.startPolling();
            renderKafkaPolling3.startPolling();

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            final Handler<RoutingContext> metricsHandler = PrometheusScrapingHandler.create();

            healthCheckHandler.register("kafka-topic-events", 2000, future -> checkTopic(healthKafkaConsumer, eventsTopic, future));
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
                        routerBuilder.operation("apidocs")
                                .handler(context -> apiV1DocsHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("health")
                                .handler(context -> healthCheckHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("metrics")
                                .handler(context -> metricsHandler.handle(RoutingContext.newInstance(context)));

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

    private void checkTopic(KafkaConsumer<String, Object> kafkaConsumer, String topic, Promise<Status> promise) {
        Single.fromCallable(() -> kafkaConsumer.partitionsFor(topic))
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
