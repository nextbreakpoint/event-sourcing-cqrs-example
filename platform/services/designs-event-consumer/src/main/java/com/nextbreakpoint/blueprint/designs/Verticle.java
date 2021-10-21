package com.nextbreakpoint.blueprint.designs;

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
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
                    .setTracingOptions(tracingOptions);

            final Vertx vertx = Vertx.vertx(vertxOptions);

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

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String eventTopic = environment.resolve(config.getString("design_event_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

            final KafkaConsumer<String, String> eventConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

            final Supplier<CassandraClient> supplier = () -> CassandraClientFactory.create(environment, vertx, config);

            final Store store = new CassandraStore(supplier);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(CONTENT_TYPE, X_XSRF_TOKEN));

            final Map<String, EventHandler<RecordAndMessage>> eventHandlers = new HashMap<>();

            eventHandlers.put(MessageType.DESIGN_INSERT_REQUESTED, createDesignInsertRequestedHandler(store, eventTopic, producer, messageSource));
            eventHandlers.put(MessageType.DESIGN_UPDATE_REQUESTED, createDesignUpdateRequestedHandler(store, eventTopic, producer, messageSource));
            eventHandlers.put(MessageType.DESIGN_DELETE_REQUESTED, createDesignDeleteRequestedHandler(store, eventTopic, producer, messageSource));

            eventHandlers.put(MessageType.AGGREGATE_UPDATE_REQUESTED, createAggregateUpdateRequestedHandler(store, eventTopic, producer, messageSource));

            eventHandlers.put(MessageType.TILE_RENDER_COMPLETED, createTileRenderCompletedHandler(store, eventTopic, producer, messageSource));

            pollRecords(eventTopic, eventConsumer, eventHandlers);

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

    private void pollRecords(String eventTopic, KafkaConsumer<String, String> eventConsumer, Map<String, EventHandler<RecordAndMessage>> eventHandlers) {
        eventConsumer.pollTimeout(Duration.ofSeconds(5))
                .fetch(10)
                .batchHandler(records -> processRecords(eventConsumer, eventHandlers, records))
                .partitionsAssignedHandler(partitions -> {})
                .partitionsRevokedHandler(partitions -> {})
                .rxSubscribe(eventTopic)
                .doOnError(err -> logger.error("Failed to consume records", err))
                .subscribe();
    }

    private void processRecords(KafkaConsumer<String, String> eventConsumer, Map<String, EventHandler<RecordAndMessage>> handlers, KafkaConsumerRecords<String, String> records) {
        final Set<TopicPartition> suspendedPartitions = new HashSet<>();

        for (int i = 0; i < records.size(); i++) {
            final KafkaConsumerRecord<String, String> record = records.recordAt(i);

            final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

            if (suspendedPartitions.contains(topicPartition)) {
                logger.debug("Skipping record of suspended partition (" + topicPartition + ")");

                continue;
            }

            final Message message = Json.decodeValue(record.value(), Message.class);

            logger.debug("Received message of type: " + message.getMessageType());

            final EventHandler<RecordAndMessage> handler = handlers.get(message.getMessageType());

            if (handler == null) {
                logger.warn("Ignoring message of type: " + message.getMessageType());

                continue;
            }

            handler.handle(new RecordAndMessage(record, message), (msg, err) -> handleError(eventConsumer, suspendedPartitions, msg));
        }

//        vertx.getOrCreateContext().runOnContext(ignore -> commitOffsets(eventConsumer));
        commitOffsets(eventConsumer);
    }

    private void handleError(KafkaConsumer<String, String> eventConsumer, Set<TopicPartition> suspendedPartitions, RecordAndMessage msg) {
        final KafkaConsumerRecord<String, String> record = msg.getRecord();

        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());

        suspendedPartitions.add(topicPartition);

        retryPartition(eventConsumer, record, topicPartition);
    }

    private void commitOffsets(KafkaConsumer<String, String> eventConsumer) {
        eventConsumer.rxCommit()
                .doOnError(err -> logger.error("Failed to commit offsets", err))
                .subscribe();
    }

    private void retryPartition(KafkaConsumer<String, String> eventConsumer, KafkaConsumerRecord<String, String> record, TopicPartition topicPartition) {
        eventConsumer.rxPause(topicPartition)
                .flatMap(x -> eventConsumer.rxSeek(topicPartition, record.offset()))
                .delay(5, TimeUnit.SECONDS)
                .flatMap(x -> eventConsumer.rxResume(topicPartition))
                .doOnError(err -> logger.error("Failed to resume partition (" + topicPartition + ")", err))
                .subscribe();
    }
}
