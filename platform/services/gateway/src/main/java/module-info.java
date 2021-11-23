module com.nextbreakpoint.blueprint.gateway {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.tracing.opentracing;
    requires vertx.rx.java;
    requires rxjava;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
}