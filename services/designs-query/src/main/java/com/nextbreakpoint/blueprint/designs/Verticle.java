package com.nextbreakpoint.blueprint.designs;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessageConsumer;
import com.nextbreakpoint.blueprint.common.drivers.KafkaMessagePolling;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.avro.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.avro.Payload;
import com.nextbreakpoint.blueprint.common.vertx.AccessHandler;
import com.nextbreakpoint.blueprint.common.vertx.CorsHandlerFactory;
import com.nextbreakpoint.blueprint.common.vertx.Initializer;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderConfig;
import com.nextbreakpoint.blueprint.common.vertx.JWTProviderFactory;
import com.nextbreakpoint.blueprint.common.vertx.OpenApiHandler;
import com.nextbreakpoint.blueprint.common.vertx.Records;
import com.nextbreakpoint.blueprint.common.vertx.ResponseHelper;
import com.nextbreakpoint.blueprint.common.vertx.Server;
import com.nextbreakpoint.blueprint.common.vertx.ServerConfig;
import com.nextbreakpoint.blueprint.designs.persistence.ElasticsearchStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.backends.BackendRegistries;
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
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.elasticsearch.client.RestClient;
import rx.Completable;
import rx.Single;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Authority.ANONYMOUS;
import static com.nextbreakpoint.blueprint.common.core.Authority.GUEST;
import static com.nextbreakpoint.blueprint.common.core.Headers.ACCEPT;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.common.core.Headers.COOKIE;
import static com.nextbreakpoint.blueprint.common.core.Headers.X_XSRF_TOKEN;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignDocumentDeleteRequestedHandler;
import static com.nextbreakpoint.blueprint.designs.Factory.createDesignDocumentUpdateRequestedHandler;

@Log4j2
public class Verticle extends AbstractVerticle {
    private KafkaMessagePolling<Payload, Object, InputMessage<Object>> kafkaPolling;

    public static void main(String[] args) {
        try {
            final JsonObject config = Initializer.loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.createVertx();

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

            final String jwtKeystoreType = config.getString("jwt_keystore_type");

            final String jwtKeystorePath = config.getString("jwt_keystore_path");

            final String jwtKeystoreSecret = config.getString("jwt_keystore_secret");

            final String s3Endpoint = config.getString("s3_endpoint");

            final String s3Bucket = config.getString("s3_bucket");

            final String s3Region = config.getString("s3_region", "eu-west-1");

            final String elasticsearchIndex = config.getString("elasticsearch_index", "designs");

            final String elasticsearchHost = config.getString("elasticsearch_host");

            final int elasticsearchPort = Integer.parseInt(config.getString("elasticsearch_port", "9092"));

            final String eventsTopic = config.getString("events_topic");

            final String messageSource = config.getString("message_source");

            final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");

            final String schemaRegistryUrl = config.getString("schema_registry_url", "http://localhost:8081");

            final String clientId = config.getString("kafka_client_id", "designs-query");

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

            final AwsCredentialsProvider credentialsProvider = AwsCredentialsProviderChain.of(DefaultCredentialsProvider.create());

            final JWTProviderConfig jwtProviderConfig = JWTProviderConfig.builder()
                    .withKeyStoreType(jwtKeystoreType)
                    .withKeyStorePath(jwtKeystorePath)
                    .withKeyStoreSecret(jwtKeystoreSecret)
                    .build();

            final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, jwtProviderConfig);

            final S3AsyncClient s3AsyncClient = S3AsyncClient.builder()
                    .region(Region.of(s3Region))
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(URI.create(s3Endpoint))
                    .forcePathStyle(true)
                    .build();

            final RestClient restClient = RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort)).build();

            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.registerModule(new JavaTimeModule());

            final ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));

            final ElasticsearchAsyncClient client = new ElasticsearchAsyncClient(transport);

            final Store store = new ElasticsearchStore(client, elasticsearchIndex);

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

            final KafkaConsumer<String, Payload> eventsKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-events").build());
            final KafkaConsumer<String, Object> healthKafkaConsumer = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-health").build());

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, List.of(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), List.of(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.response().setStatusCode(403).setStatusMessage("Access denied").end();

            final Handler<RoutingContext> listDesignsHandler = new AccessHandler(jwtProvider, Factory.createListDesignsHandler(store), onAccessDenied, List.of(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext> loadDesignHandler = new AccessHandler(jwtProvider, Factory.createLoadDesignHandler(store), onAccessDenied, List.of(ADMIN, GUEST, ANONYMOUS));

            final Handler<RoutingContext> getTileHandler = new AccessHandler(jwtProvider, Factory.createGetTileHandler(store, s3AsyncClient, s3Bucket), onAccessDenied, List.of(ADMIN, GUEST, ANONYMOUS));

            final Map<String, RxSingleHandler<InputMessage<Object>, Void>> messageHandlers = new HashMap<>();

            messageHandlers.put(DesignDocumentUpdateRequested.getClassSchema().getFullName(), createDesignDocumentUpdateRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers.put(DesignDocumentDeleteRequested.getClassSchema().getFullName(), createDesignDocumentDeleteRequestedHandler(store, eventsTopic, kafkaProducer, messageSource));

            eventsKafkaConsumer.subscribe(List.of(eventsTopic));

            new KafkaClientMetrics(kafkaProducer).bindTo(registry);
            new KafkaClientMetrics(healthKafkaConsumer).bindTo(registry);
            new KafkaClientMetrics(eventsKafkaConsumer).bindTo(registry);

            kafkaPolling = new KafkaMessagePolling<>(eventsKafkaConsumer, Records.createEventInputRecordMapper(), messageHandlers, KafkaMessageConsumer.Simple.create(messageHandlers, registry), registry);

            kafkaPolling.startPolling();

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            final Handler<RoutingContext> metricsHandler = PrometheusScrapingHandler.create();

            healthCheckHandler.register("kafka-topic-events", 2000, future -> checkTopic(healthKafkaConsumer, eventsTopic, future));
            healthCheckHandler.register("elasticsearch-index-main", 2000, future -> checkTable(store, future, "designs"));
            healthCheckHandler.register("elasticsearch-index-draft", 2000, future -> checkTable(store, future, "designs_draft"));
            healthCheckHandler.register("bucket", 2000, future -> checkBucket(s3AsyncClient, s3Bucket, future));

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

                        routerBuilder.operation("getTile")
                                .handler(context -> getTileHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("listDesigns")
                                .handler(context -> listDesignsHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("loadDesign")
                                .handler(context -> loadDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("tileOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        routerBuilder.operation("designOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        routerBuilder.operation("designsOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

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

    private void checkBucket(S3AsyncClient s3AsyncClient, String bucket, Promise<Status> promise) {
        s3AsyncClient.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).maxKeys(1).build())
                .orTimeout(1, TimeUnit.SECONDS)
                .whenComplete((buckets, err) -> {
                    if (err != null) {
                        promise.complete(Status.KO());
                    } else {
                        promise.complete(Status.OK());
                    }
                });
    }

    private void checkTable(Store store, Promise<Status> promise, String indexName) {
        store.existsIndex(indexName)
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(exists -> promise.complete(exists ? Status.OK() : Status.KO()), err -> promise.complete(Status.KO()));
    }

    private void checkTopic(KafkaConsumer<String, Object> kafkaConsumer, String topic, Promise<Status> promise) {
        Single.fromCallable(() -> kafkaConsumer.partitionsFor(topic))
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
