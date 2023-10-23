package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.core.RxSingleHandler;
import com.nextbreakpoint.blueprint.common.drivers.KafkaClientFactory;
import com.nextbreakpoint.blueprint.common.drivers.KafkaConsumerConfig;
import com.nextbreakpoint.blueprint.common.drivers.KafkaProducerConfig;
import com.nextbreakpoint.blueprint.common.drivers.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.*;
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
import io.vertx.rxjava.core.WorkerExecutor;
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
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
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
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.createTileRenderRequestedHandler;

@Log4j2
public class Verticle extends AbstractVerticle {
    private KafkaMessagePolling renderKafkaPolling0;
    private KafkaMessagePolling renderKafkaPolling1;
    private KafkaMessagePolling renderKafkaPolling2;
    private KafkaMessagePolling renderKafkaPolling3;

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

            final int poolSize = Runtime.getRuntime().availableProcessors();

            final long maxExecuteTime = Integer.parseInt(config.getString("max_execution_time_in_millis", "5000000"));

            final WorkerExecutor workerExecutor = vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime, TimeUnit.MICROSECONDS);

            final int port = Integer.parseInt(config.getString("host_port"));

            final String originPattern = config.getString("origin_pattern");

            final String jksStorePath = config.getString("server_keystore_path");

            final String jksStoreSecret = config.getString("server_keystore_secret");

            final String s3Endpoint = config.getString("s3_endpoint");

            final String s3Bucket = config.getString("s3_bucket");

            final String s3Region = config.getString("s3_region", "eu-west-1");

            final String jwtKeystoreType = config.getString("jwt_keystore_type");

            final String jwtKeystorePath = config.getString("jwt_keystore_path");

            final String jwtKeystoreSecret = config.getString("jwt_keystore_secret");

            final String messageSource = config.getString("message_source");

            final String renderTopicPrefix = config.getString("render_topic_prefix");

            final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");

            final String keySerializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringSerializer");

            final String valSerializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringSerializer");

            final String clientId = config.getString("kafka_client_id", "designs-render");

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

            final MeterRegistry registry = BackendRegistries.getDefaultNow();

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
            final KafkaConsumer<String, String> renderKafkaConsumer0 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-requested-0").build());
            final KafkaConsumer<String, String> renderKafkaConsumer1 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-requested-1").build());
            final KafkaConsumer<String, String> renderKafkaConsumer2 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-requested-2").build());
            final KafkaConsumer<String, String> renderKafkaConsumer3 = KafkaClientFactory.createConsumer(consumerConfig.toBuilder().withGroupId(groupId + "-render-requested-3").build());

            final AwsCredentialsProvider credentialsProvider = AwsCredentialsProviderChain.of(DefaultCredentialsProvider.create());

            final S3AsyncClient s3AsyncClient = S3AsyncClient.builder()
                    .region(Region.of(s3Region))
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(URI.create(s3Endpoint))
                    .build();

            final JWTProviderConfig jwtProviderConfig = JWTProviderConfig.builder()
                    .withKeyStoreType(jwtKeystoreType)
                    .withKeyStorePath(jwtKeystorePath)
                    .withKeyStoreSecret(jwtKeystoreSecret)
                    .build();

            final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, jwtProviderConfig);

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers = new HashMap<>();

            messageHandlers.put(TileRenderRequested.TYPE, createTileRenderRequestedHandler(renderTopicPrefix, kafkaProducer, messageSource, workerExecutor, s3AsyncClient, s3Bucket));

            renderKafkaConsumer0.subscribe(Set.of(renderTopicPrefix + "-requested-0"));
            renderKafkaConsumer1.subscribe(Set.of(renderTopicPrefix + "-requested-1"));
            renderKafkaConsumer2.subscribe(Set.of(renderTopicPrefix + "-requested-2"));
            renderKafkaConsumer3.subscribe(Set.of(renderTopicPrefix + "-requested-3"));

            new KafkaClientMetrics(kafkaProducer).bindTo(registry);
            new KafkaClientMetrics(healthKafkaConsumer).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer0).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer1).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer2).bindTo(registry);
            new KafkaClientMetrics(renderKafkaConsumer3).bindTo(registry);

            renderKafkaPolling0 = new KafkaMessagePolling<>(renderKafkaConsumer0, messageHandlers, KafkaMessageConsumer.Simple.create(messageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);
            renderKafkaPolling1 = new KafkaMessagePolling<>(renderKafkaConsumer1, messageHandlers, KafkaMessageConsumer.Simple.create(messageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);
            renderKafkaPolling2 = new KafkaMessagePolling<>(renderKafkaConsumer2, messageHandlers, KafkaMessageConsumer.Simple.create(messageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);
            renderKafkaPolling3 = new KafkaMessagePolling<>(renderKafkaConsumer3, messageHandlers, KafkaMessageConsumer.Simple.create(messageHandlers, registry), registry, KafkaRecordsQueue.Compacted.create(), -1, 10);

            renderKafkaPolling0.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-requested-0");
            renderKafkaPolling1.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-requested-1");
            renderKafkaPolling2.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-requested-2");
            renderKafkaPolling3.startPolling("kafka-polling-topic-" + renderTopicPrefix + "-requested-3");

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, List.of(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), List.of(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.response().setStatusCode(403).setStatusMessage("Access denied").end();

            final Handler<RoutingContext> validateDesignHandler = new AccessHandler(jwtProvider, Factory.createValidateDesignHandler(), onAccessDenied, List.of(ADMIN));

            final Handler<RoutingContext> downloadDesignHandler = new AccessHandler(jwtProvider, Factory.createDownloadDesignHandler(), onAccessDenied, List.of(ADMIN));

            final Handler<RoutingContext> uploadDesignHandler = new AccessHandler(jwtProvider, Factory.createUploadDesignHandler(), onAccessDenied, List.of(ADMIN));

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            healthCheckHandler.register("kafka-topic-render-requested-0", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-0", future));
            healthCheckHandler.register("kafka-topic-render-requested-1", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-1", future));
            healthCheckHandler.register("kafka-topic-render-requested-2", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-2", future));
            healthCheckHandler.register("kafka-topic-render-requested-3", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-requested-3", future));
            healthCheckHandler.register("kafka-topic-render-completed-0", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-0", future));
            healthCheckHandler.register("kafka-topic-render-completed-1", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-1", future));
            healthCheckHandler.register("kafka-topic-render-completed-2", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-2", future));
            healthCheckHandler.register("kafka-topic-render-completed-3", 2000, future -> checkTopic(healthKafkaConsumer, renderTopicPrefix + "-completed-3", future));
            healthCheckHandler.register("bucket-tiles", 2000, future -> checkBucket(s3AsyncClient, s3Bucket, future));

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
                                .handler(context -> PrometheusScrapingHandler.create().handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("validateDesign")
                                .handler(context -> validateDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("downloadDesign")
                                .handler(context -> downloadDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("uploadDesign")
                                .handler(context -> uploadDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("validateDesignOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        routerBuilder.operation("downloadDesignOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        routerBuilder.operation("uploadDesignOptions")
                                .handler(context -> ResponseHelper.sendNoContent(RoutingContext.newInstance(context)));

                        final Router router = Router.newInstance(routerBuilder.createRouter());

                        final Router mainRouter = Router.router(vertx);

                        mainRouter.route().handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
                        mainRouter.route().handler(TimeoutHandler.create(30000));
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

    private void checkTopic(KafkaConsumer<String, String> kafkaConsumer, String eventsTopic, Promise<Status> promise) {
        Single.fromCallable(() -> kafkaConsumer.partitionsFor(eventsTopic))
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
