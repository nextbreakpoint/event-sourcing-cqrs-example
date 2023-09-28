module com.nextbreakpoint.blueprint.accounts {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.healthcheck;
    requires io.vertx.client.jdbc;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.tracing.opentelemetry;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.context;
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.exporter.jaeger;
    requires io.opentelemetry.exporter.otlp.trace;
    requires org.jctools.core;
    requires vertx.rx.java;
    requires rxjava;
    requires java.sql;
    requires static lombok;
}