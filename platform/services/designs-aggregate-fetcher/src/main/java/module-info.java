module com.nextbreakpoint.blueprint.designsaggregatefetcher {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires com.nextbreakpoint.blueprint.common.cassandra;
    requires transitive com.nextbreakpoint.nextfractal.core;
    requires transitive com.nextbreakpoint.nextfractal.mandelbrot;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    opens com.nextbreakpoint.blueprint.designs to com.nextbreakpoint.blueprint.common.vertx, com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.blueprint.designs.model to com.fasterxml.jackson.databind;
}