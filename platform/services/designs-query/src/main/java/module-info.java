module com.nextbreakpoint.blueprint.designs.query {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.nextbreakpoint.nextfractal.core;
    requires com.nextbreakpoint.nextfractal.mandelbrot;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.tracing.opentracing;
    requires io.vertx.client.cassandra;
    requires vertx.rx.java;
    requires rxjava;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires com.datastax.oss.driver.core;
    requires java.sql;
    requires java.desktop;
    requires jdk.compiler;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.s3;
    opens com.nextbreakpoint.blueprint.designs.model to com.fasterxml.jackson.databind;
}