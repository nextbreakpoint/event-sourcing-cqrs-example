module com.nextbreakpoint.blueprint.accounts {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.client.jdbc;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.tracing.opentracing;
    requires vertx.rx.java;
    requires rxjava;
    requires org.apache.logging.log4j;
    requires java.sql;
}