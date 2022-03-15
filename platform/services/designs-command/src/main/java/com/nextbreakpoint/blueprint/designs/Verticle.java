package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.commands.DesignDeleteCommand;
import com.nextbreakpoint.blueprint.common.commands.DesignInsertCommand;
import com.nextbreakpoint.blueprint.common.commands.DesignUpdateCommand;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.InputMessage;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.persistence.CassandraStore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import rx.Completable;
import rx.plugins.RxJavaHooks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.nextbreakpoint.blueprint.common.core.Authority.ADMIN;
import static com.nextbreakpoint.blueprint.common.core.Headers.*;
import static com.nextbreakpoint.blueprint.designs.Factory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class Verticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Verticle.class.getName());

    private KafkaPolling kafkaPolling;

    public static void main(String[] args) {
        try {
            final JsonObject config = loadConfig(args.length > 0 ? args[0] : "config/localhost.json");

            final Vertx vertx = Initializer.initialize();

            RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
            RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));
            RxJavaHooks.setOnNewThreadScheduler(s -> RxHelper.blockingScheduler(vertx));

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

            final String commandsTopic = config.getString("commands_topic");

            final String eventsTopic = config.getString("events_topic");

            final String messageSource = config.getString("message_source");

            final String jwtKeystoreType = config.getString("jwt_keystore_type");

            final String jwtKeystorePath = config.getString("jwt_keystore_path");

            final String jwtKeystoreSecret = config.getString("jwt_keystore_secret");

            final String clusterName = config.getString("cassandra_cluster");

            final String keyspace = config.getString("cassandra_keyspace");

            final String username = config.getString("cassandra_username");

            final String password = config.getString("cassandra_password");

            final String[] contactPoints = config.getString("cassandra_contactPoints").split(",");

            final int cassandraPort = Integer.parseInt(config.getString("cassandra_port", "9042"));

            final String bootstrapServers = config.getString("kafka_bootstrap_servers", "localhost:9092");

            final String keySerializer = config.getString("kafka_key_serializer", "org.apache.kafka.common.serialization.StringSerializer");

            final String valSerializer = config.getString("kafka_val_serializer", "org.apache.kafka.common.serialization.StringSerializer");

            final String clientId = config.getString("kafka_client_id", "designs-command");

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

            final JWTProviderConfig jwtProviderConfig = JWTProviderConfig.builder()
                    .withKeyStoreType(jwtKeystoreType)
                    .withKeyStorePath(jwtKeystorePath)
                    .withKeyStoreSecret(jwtKeystoreSecret)
                    .build();

            final JWTAuth jwtProvider = JWTProviderFactory.create(vertx, jwtProviderConfig);

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
                    .withGroupId(groupId)
                    .withAutoOffsetReset(autoOffsetReset)
                    .withEnableAutoCommit(enableAutoCommit)
                    .build();

            final KafkaConsumer<String, String> kafkaConsumer = KafkaClientFactory.createConsumer(vertx, consumerConfig);

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

            kafkaConsumer.subscribe(commandsTopic);

            final Map<String, RxSingleHandler<InputMessage, ?>> messageHandlers = new HashMap<>();

            messageHandlers.put(DesignInsertCommand.TYPE, createDesignInsertCommandHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers.put(DesignUpdateCommand.TYPE, createDesignUpdateCommandHandler(store, eventsTopic, kafkaProducer, messageSource));
            messageHandlers.put(DesignDeleteCommand.TYPE, createDesignDeleteCommandHandler(store, eventsTopic, kafkaProducer, messageSource));

            kafkaPolling = new KafkaPolling(kafkaConsumer, messageHandlers);

            kafkaPolling.startPolling("kafka-polling-topic-" + commandsTopic);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(COOKIE, AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(COOKIE, CONTENT_TYPE, X_XSRF_TOKEN));

            final Handler<RoutingContext> onAccessDenied = routingContext -> routingContext.fail(Failure.accessDenied("Authorisation failed"));

            final Handler<RoutingContext> insertDesignHandler = new AccessHandler(jwtProvider, Factory.createInsertDesignHandler(kafkaProducer, commandsTopic, messageSource), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> updateDesignHandler = new AccessHandler(jwtProvider, Factory.createUpdateDesignHandler(kafkaProducer, commandsTopic, messageSource), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> deleteDesignHandler = new AccessHandler(jwtProvider, Factory.createDeleteDesignHandler(kafkaProducer, commandsTopic, messageSource), onAccessDenied, singletonList(ADMIN));

            final Handler<RoutingContext> apiV1DocsHandler = new OpenApiHandler(vertx.getDelegate(), executor, "api-v1.yaml");

            final URL resource = RouterBuilder.class.getClassLoader().getResource("api-v1.yaml");

            if (resource == null) {
                throw new Exception("Cannot find resource api-v1.yaml");
            }

            final File tempFile = File.createTempFile("openapi-", ".yaml");

            IOUtils.copy(resource.openStream(), new FileOutputStream(tempFile), StandardCharsets.UTF_8);

            RouterBuilder.create(vertx.getDelegate(), "file://" + tempFile.getAbsolutePath())
                    .onSuccess(routerBuilder -> {
                        routerBuilder.operation("insertDesign")
                                .handler(context -> insertDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("updateDesign")
                                .handler(context -> updateDesignHandler.handle(RoutingContext.newInstance(context)));

                        routerBuilder.operation("deleteDesign")
                                .handler(context -> deleteDesignHandler.handle(RoutingContext.newInstance(context)));

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
