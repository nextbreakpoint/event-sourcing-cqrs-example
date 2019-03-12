module com.nextbreakpoint.shop.designs {
    requires com.nextbreakpoint.shop.common;
    requires transitive com.nextbreakpoint.nextfractal.core;
    requires transitive com.nextbreakpoint.nextfractal.mandelbrot;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.shop.designs to com.nextbreakpoint.shop.common, com.fasterxml.jackson.databind;
}