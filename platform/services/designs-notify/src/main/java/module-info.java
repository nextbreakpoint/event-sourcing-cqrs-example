module com.nextbreakpoint.blueprint.designs.notify {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.nextbreakpoint.blueprint.common.events;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.healthcheck;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.client.kafka;
    requires io.vertx.servicediscovery;
    requires io.vertx.servicediscovery.bridge.consul;
    requires io.vertx.tracing.opentelemetry;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.context;
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.exporter.jaeger;
    requires io.opentelemetry.exporter.otlp.trace;
    requires vertx.rx.java;
    requires rxjava;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires static lombok;
    opens com.nextbreakpoint.blueprint.designs.model to com.fasterxml.jackson.databind;
}