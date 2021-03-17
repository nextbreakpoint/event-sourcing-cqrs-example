package com.nextbreakpoint.blueprint.designs;

import com.datastax.driver.core.Session;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.MessageType;
import com.nextbreakpoint.blueprint.common.vertx.*;
import com.nextbreakpoint.blueprint.designs.common.CommandFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.CommandSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.common.ViewFailureConsumer;
import com.nextbreakpoint.blueprint.designs.common.ViewSuccessConsumer;
import com.nextbreakpoint.blueprint.designs.model.RecordAndMessage;
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
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Promise;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
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

import java.io.FileInputStream;
import java.io.IOException;
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
        return vertx.rxExecuteBlocking(this::initServer).toCompletable();
    }

    private void initServer(Promise<Void> promise) {
        try {
            final JsonObject config = vertx.getOrCreateContext().config();

            final Environment environment = Environment.getDefaultEnvironment();

            final Executor executor = Executors.newSingleThreadExecutor();

            final int port = Integer.parseInt(environment.resolve(config.getString("host_port")));

            final String originPattern = environment.resolve(config.getString("origin_pattern"));

            final String keyspace = environment.resolve(config.getString("cassandra_keyspace"));

            final String eventsTopic = environment.resolve(config.getString("events_topic"));

            final String sseTopic = environment.resolve(config.getString("sse_topic"));

            final String viewTopic = environment.resolve(config.getString("view_topic"));

            final String messageSource = environment.resolve(config.getString("message_source"));

            final JWTAuth jwtProvider = JWTProviderFactory.create(environment, vertx, config);

            final KafkaProducer<String, String> producer = KafkaClientFactory.createProducer(environment, vertx, config);

            final KafkaConsumer<String, String> commandConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

            final KafkaConsumer<String, String> viewConsumer = KafkaClientFactory.createConsumer(environment, vertx, config);

            final Supplier<Session> supplier = () -> CassandraClusterFactory.create(environment, config).connect(keyspace);

            final Store store = new CassandraStore(supplier);

            final Router mainRouter = Router.router(vertx);

            final CorsHandler corsHandler = CorsHandlerFactory.createWithAll(originPattern, asList(AUTHORIZATION, CONTENT_TYPE, ACCEPT, X_XSRF_TOKEN, X_MODIFIED), asList(CONTENT_TYPE, X_XSRF_TOKEN, X_MODIFIED));

            final Map<String, Handler<RecordAndMessage>> commandHandlers = new HashMap<>();

            commandHandlers.put(MessageType.DESIGN_INSERT, createInsertDesignHandler(store, viewTopic, producer, messageSource, new CommandSuccessConsumer(commandConsumer), new CommandFailureConsumer(commandConsumer)));
            commandHandlers.put(MessageType.DESIGN_UPDATE, createUpdateDesignHandler(store, viewTopic, producer, messageSource, new CommandSuccessConsumer(commandConsumer), new CommandFailureConsumer(commandConsumer)));
            commandHandlers.put(MessageType.DESIGN_DELETE, createDeleteDesignHandler(store, viewTopic, producer, messageSource, new CommandSuccessConsumer(commandConsumer), new CommandFailureConsumer(commandConsumer)));

            final Map<String, Handler<RecordAndMessage>> viewHandlers = new HashMap<>();

            viewHandlers.put(MessageType.DESIGN_CHANGED, createDesignChangedHandler(store, sseTopic, producer, messageSource, new ViewSuccessConsumer(viewConsumer), new ViewFailureConsumer(viewConsumer)));

            commandConsumer.handler(record -> processRecord(commandHandlers, record))
                    .rxSubscribe(eventsTopic)
                    .doOnError(this::handleError)
                    .subscribe();

            viewConsumer.handler(record -> processRecord(viewHandlers, record))
                    .rxSubscribe(viewTopic)
                    .doOnError(this::handleError)
                    .subscribe();

            final Handler<RoutingContext> openapiHandler = new OpenApiHandler(vertx.getDelegate(), executor, "openapi.yaml");

            final String url = RouterBuilder.class.getClassLoader().getResource("openapi.yaml").toURI().toString();

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

                        mainRouter.get("/v1/apidocs").handler(openapiHandler::handle);

                        mainRouter.options("/*").handler(ResponseHelper::sendNoContent);

                        mainRouter.route().failureHandler(ResponseHelper::sendFailure);

                        final HttpServerOptions options = ServerUtil.makeServerOptions(environment, config);

                        vertx.createHttpServer(options)
                                .requestHandler(mainRouter::handle)
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
            logger.warn("Can't handle message with type: " + message.getMessageType());
        }
    }
}
