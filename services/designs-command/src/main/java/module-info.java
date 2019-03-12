module com.nextbreakpoint.shop.designs.command {
    requires com.nextbreakpoint.shop.common;
    requires com.nextbreakpoint.nextfractal.core;
    requires com.nextbreakpoint.nextfractal.mandelbrot;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.shop.designs to com.nextbreakpoint.shop.common, com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.shop.designs.model to com.fasterxml.jackson.databind;
}