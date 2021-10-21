package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
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
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.WorkerExecutor;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;
import io.vertx.rxjava.ext.web.handler.TimeoutHandler;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava.kafka.client.producer.KafkaProducer;
import io.vertx.tracing.opentracing.OpenTracingOptions;
import rx.Completable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

            final WorkerExecutor workerExecutor = createWorkerExecutor(environment, config);

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String imageBucket = environment.resolve(config.getString("s3_bucket"));

            final String eventTopic = environment.resolve(config.getString("design_event_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final String s3Endpoint = environment.resolve(config.getString("s3_endpoint"));

            final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

            final KafkaConsumer<String, String> eventConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

            final AwsCredentialsProvider credentialsProvider = AwsCredentialsProviderChain.of(DefaultCredentialsProvider.create());

            final S3AsyncClient s3AsyncClient = S3AsyncClient.builder()
                    .region(Region.EU_WEST_1)
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(URI.create(s3Endpoint))
                    .build();

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN), asList(CONTENT_TYPE, X_XSRF_TOKEN));

            final Map<String, Handler<RecordAndMessage>> eventHandlers = new HashMap<>();

            eventHandlers.put(MessageType.TILE_RENDER_REQUESTED, createTileCreatedHandler(workerExecutor, s3AsyncClient, imageBucket, eventTopic, producer, messageSource));

            eventConsumer.handler(record -> processRecord(eventHandlers, record))
                    .rxSubscribe(eventTopic)
                    .doOnError(this::handleError)
                    .subscribe();

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

    private void handleError(Throwable err) {
        logger.error("Cannot process message", err);
    }

    private void processRecord(Map<String, Handler<RecordAndMessage>> handlers, KafkaConsumerRecord<String, String> record) {
        final Message message = Json.decodeValue(record.value(), Message.class);
        final Handler<RecordAndMessage> handler = handlers.get(message.getMessageType());
        if (handler != null) {
            logger.info("Receive message of type: " + message.getMessageType());
            handler.handle(new RecordAndMessage(record, message));
        } else {
            logger.info("Ignore message of type: " + message.getMessageType());
        }
    }

    private WorkerExecutor createWorkerExecutor(Environment environment, JsonObject config) {
        final int poolSize = Runtime.getRuntime().availableProcessors();
        final long maxExecuteTime = Integer.parseInt(environment.resolve(config.getString("max_execution_time_in_millis", "2000"))) * 1000000L;
        return vertx.createSharedWorkerExecutor("worker", poolSize, maxExecuteTime);
    }
}