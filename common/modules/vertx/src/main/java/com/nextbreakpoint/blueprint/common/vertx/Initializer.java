package com.nextbreakpoint.blueprint.common.vertx;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;
import rx.plugins.RxJavaHooks;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Initializer {
    private Initializer() {}

    public static Vertx createVertx() {
        final VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
                .setPublishQuantiles(true)
                .setEnabled(true);

        final MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                .setPrometheusOptions(prometheusOptions)
                .setEnabled(true);

        final JaegerGrpcSpanExporter spanExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(Optional.ofNullable(System.getenv("JAEGER_ENDPOINT")).orElse("http://localhost:14250"))
                .build();

        final Resource resource = getResource(Optional.ofNullable(System.getenv("JAEGER_ATTRIBUTES")).orElse(""));

        final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();

        final OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        final OpenTelemetryOptions tracingOptions = new OpenTelemetryOptions(openTelemetry);

        final AddressResolverOptions addressResolverOptions = new AddressResolverOptions()
                .setCacheNegativeTimeToLive(0)
                .setCacheMaxTimeToLive(30);

        final VertxOptions vertxOptions = new VertxOptions()
                .setAddressResolverOptions(addressResolverOptions)
                .setMetricsOptions(metricsOptions)
                .setTracingOptions(tracingOptions)
                .setBlockedThreadCheckInterval(5000)
                .setBlockedThreadCheckIntervalUnit(TimeUnit.SECONDS)
                .setWorkerPoolSize(20);

        ContextStorage.addWrapper(CustomContextStorage::new);

        final Vertx vertx = Vertx.vertx(vertxOptions);

        RxJavaHooks.setOnComputationScheduler(s -> RxHelper.scheduler(vertx));
        RxJavaHooks.setOnIOScheduler(s -> RxHelper.blockingScheduler(vertx));

        MeterRegistry registry = BackendRegistries.getDefaultNow();

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);

        return vertx;
    }

    private static Resource getResource(String attributes) {
        final Pattern pattern = Pattern.compile("(.+)=(.+)");

        final Map<String, String> attributeMap = Arrays.stream(attributes.split(","))
                .map(String::trim)
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(matcher -> matcher.group(1), matcher -> matcher.group(2)));

        final ResourceBuilder builder = Resource.getDefault().toBuilder();

        attributeMap.forEach(builder::put);

        return builder.build();
    }

    private static class CustomContextStorage implements ContextStorage {
        private ContextStorage vertxContextStorage;

        private CustomContextStorage(ContextStorage vertxContextStorage) {
            this.vertxContextStorage = vertxContextStorage;
        }

        @Override
        public Scope attach(Context toAttach) {
            if (Vertx.currentContext() != null && Vertx.currentContext().isEventLoopContext()) {
                return vertxContextStorage.attach(toAttach);
            } else {
                return ContextStorage.defaultStorage().attach(toAttach);
            }
        }

        @Override
        public Context current() {
            if (Vertx.currentContext() != null && Vertx.currentContext().isEventLoopContext()) {
                return vertxContextStorage.current();
            } else {
                return ContextStorage.defaultStorage().current();
            }
        }

        @Override
        public Context root() {
            if (Vertx.currentContext() != null && Vertx.currentContext().isEventLoopContext()) {
                return vertxContextStorage.root();
            } else {
                return ContextStorage.defaultStorage().root();
            }
        }
    }

    public static JsonObject loadConfig(String configPath) throws IOException {
        final Environment environment = Environment.getDefaultEnvironment();

        try (FileInputStream stream = new FileInputStream(configPath)) {
            return new JsonObject(environment.resolve(IOUtils.toString(stream)));
        }
    }
}
