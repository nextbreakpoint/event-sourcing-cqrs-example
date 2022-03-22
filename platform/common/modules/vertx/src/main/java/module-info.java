module com.nextbreakpoint.blueprint.common.vertx {
    requires static com.nextbreakpoint.blueprint.common.core;
    requires static io.vertx.core;
    requires static io.vertx.auth.common;
    requires static io.vertx.auth.jwt;
    requires static io.vertx.web.client;
    requires static io.vertx.web.openapi;
    requires static io.vertx.client.kafka;
    requires static io.vertx.client.cassandra;
    requires static io.vertx.tracing.opentelemetry;
    requires static io.vertx.metrics.micrometer;
    requires static micrometer.core;
    requires static io.opentelemetry.semconv;
    requires static io.opentelemetry.context;
    requires static io.opentelemetry.sdk.common;
    requires static io.opentelemetry.api;
    requires static io.opentelemetry.sdk;
    requires static io.opentelemetry.sdk.trace;
    requires static io.opentelemetry.exporter.jaeger;
    requires static io.opentelemetry.exporter.otlp.trace;
    requires static vertx.rx.java;
    requires static kafka.clients;
    requires static com.datastax.oss.driver.core;
    requires static rxjava;
    requires static lombok;
    exports com.nextbreakpoint.blueprint.common.vertx;
}