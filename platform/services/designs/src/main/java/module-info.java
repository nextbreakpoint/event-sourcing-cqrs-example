module com.nextbreakpoint.blueprint.designs {
    requires com.nextbreakpoint.blueprint.common.core;
    requires com.nextbreakpoint.blueprint.common.vertx;
    requires transitive com.nextbreakpoint.nextfractal.core;
    requires transitive com.nextbreakpoint.nextfractal.mandelbrot;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.sql;
    opens com.nextbreakpoint.blueprint.designs to com.nextbreakpoint.blueprint.common.vertx, com.fasterxml.jackson.databind;
}