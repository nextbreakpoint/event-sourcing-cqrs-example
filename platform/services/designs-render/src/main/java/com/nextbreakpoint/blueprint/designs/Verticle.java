package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
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
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava.micrometer.PrometheusScrapingHandler;
import rx.Completable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.io.File;
import java.io.FileOutputStream;
import java.io.LineNumberInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.createTileRenderRequestedHandler;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    private KafkaPolling kafkaPolling1;
    private KafkaPolling kafkaPolling2;
    private KafkaPolling kafkaPolling3;
    private KafkaPolling kafkaPolling4;
    private KafkaPolling kafkaPolling5;

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
            if (kafkaPolling1 != null) {
                kafkaPolling1.stopPolling();
            }
            if (kafkaPolling2 != null) {
                kafkaPolling2.stopPolling();
            }
            if (kafkaPolling3 != null) {
                kafkaPolling3.stopPolling();
            }
            if (kafkaPolling4 != null) {
                kafkaPolling4.stopPolling();
            }
            if (kafkaPolling5 != null) {
                kafkaPolling5.stopPolling();
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

            final String renderTopic = config.getString("render_topic");

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
            final KafkaConsumer<String, String> kafkaConsumer5 = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-5").build());
            final KafkaConsumer<String, String> kafkaConsumer6 = KafkaClientFactory.createConsumer(vertx, consumerConfig.toBuilder().withGroupId(groupId + "-6").build());

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

            final Router mainRouter = Router.router(vertx);

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers = new HashMap<>();

            messageHandlers.put(TileRenderRequested.TYPE, createTileRenderRequestedHandler(renderTopic, kafkaProducer, messageSource, workerExecutor, s3AsyncClient, s3Bucket));

            kafkaConsumer1.subscribe(Set.of(renderTopic + "-0"));
            kafkaConsumer2.subscribe(Set.of(renderTopic + "-1"));
            kafkaConsumer3.subscribe(Set.of(renderTopic + "-2"));
            kafkaConsumer4.subscribe(Set.of(renderTopic + "-3"));
            kafkaConsumer5.subscribe(Set.of(renderTopic + "-4"));

            kafkaPolling1 = new KafkaPolling(kafkaConsumer1, messageHandlers, KafkaRecordsQueue.Compacted.create(), -1, 20);
            kafkaPolling2 = new KafkaPolling(kafkaConsumer2, messageHandlers, KafkaRecordsQueue.Compacted.create(), -1, 20);
            kafkaPolling3 = new KafkaPolling(kafkaConsumer3, messageHandlers, KafkaRecordsQueue.Compacted.create(), -1, 20);
            kafkaPolling4 = new KafkaPolling(kafkaConsumer4, messageHandlers, KafkaRecordsQueue.Compacted.create(), -1, 20);
            kafkaPolling5 = new KafkaPolling(kafkaConsumer5, messageHandlers, KafkaRecordsQueue.Compacted.create(), -1, 20);

            kafkaPolling1.startPolling("kafka-polling-topic-" + renderTopic + "-1");
            kafkaPolling2.startPolling("kafka-polling-topic-" + renderTopic + "-2");
            kafkaPolling3.startPolling("kafka-polling-topic-" + renderTopic + "-3");
            kafkaPolling4.startPolling("kafka-polling-topic-" + renderTopic + "-4");
            kafkaPolling5.startPolling("kafka-polling-topic-" + renderTopic + "-5");

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext> validateDesignHandler = new AccessHandler(jwtProvider, Factory.createValidateDesignHandler(), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> downloadDesignHandler = new AccessHandler(jwtProvider, Factory.createDownloadDesignHandler(), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> uploadDesignHandler = new AccessHandler(jwtProvider, Factory.createUploadDesignHandler(), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx));

            healthCheckHandler.register("kafka-topic-render", 2000, future -> checkTopic(kafkaConsumer6, renderTopic, future));
            healthCheckHandler.register("bucket-tiles", 2000, future -> checkBucket(s3AsyncClient, s3Bucket, future));

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("validateDesign")
                                .handler(context -> validateDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("downloadDesign")
                                .handler(context -> downloadDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("uploadDesign")
                                .handler(context -> uploadDesignHandler.handle(RoutingContext.newInstance(context)));

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
        kafkaConsumer.rxPartitionsFor(eventsTopic)
                .timeout(1, TimeUnit.SECONDS)
                .subscribe(partitions -> promise.complete(Status.OK()), err -> promise.complete(Status.KO()));
    }
}
