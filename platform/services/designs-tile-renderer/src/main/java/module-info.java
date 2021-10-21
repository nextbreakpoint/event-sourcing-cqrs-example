module com.nextbreakpoint.blueprint.designstilerender {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires transitive com.nextbreakpoint.nextfractal.core;
    requires transitive com.nextbreakpoint.nextfractal.mandelbrot;
    requires io.vertx.core;
    requires io.vertx.web;
    requires io.vertx.web.openapi;
    requires io.vertx.metrics.micrometer;
    requires io.vertx.tracing.opentracing;
    requires vertx.rx.java;
    requires rxjava;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires java.desktop;
    requires jdk.compiler;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.s3;
    opens com.nextbreakpoint.blueprint.designs.model to com.fasterxml.jackson.databind;
    exports com.nextbreakpoint.blueprint.designs to com.fasterxml.jackson.databind;
}