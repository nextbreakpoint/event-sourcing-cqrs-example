module com.nextbreakpoint.blueprint.common.drivers {
    requires static com.nextbreakpoint.blueprint.common.core;
    requires static com.nextbreakpoint.blueprint.confluent;
    requires static io.opentelemetry.semconv;
    requires static io.opentelemetry.context;
    requires static io.opentelemetry.sdk.common;
    requires static io.opentelemetry.api;
    requires static io.opentelemetry.sdk;
    requires static io.opentelemetry.sdk.trace;
    requires static io.opentelemetry.exporter.jaeger;
    requires static io.opentelemetry.exporter.otlp.trace;
    requires static kafka.clients;
    requires static micrometer.core;
    requires static com.datastax.oss.driver.core;
    requires static lombok;
    requires static rxjava;
    requires static org.apache.logging.log4j;
    exports com.nextbreakpoint.blueprint.common.drivers;
}