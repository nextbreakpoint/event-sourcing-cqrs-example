module com.nextbreakpoint.blueprint.designscommandproducer {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.nextbreakpoint.blueprint.common.cassandra;
    requires com.nextbreakpoint.nextfractal.core;
    requires com.nextbreakpoint.nextfractal.mandelbrot;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.blueprint.designs to com.nextbreakpoint.blueprint.common, com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.blueprint.designs.model to com.fasterxml.jackson.databind;
}