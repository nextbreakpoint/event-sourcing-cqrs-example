module com.nextbreakpoint.blueprint.authentication {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.auth.jwt;
    requires io.vertx.auth.oauth2;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.tracing.opentracing;
    requires vertx.rx.java;
    requires rxjava;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
}