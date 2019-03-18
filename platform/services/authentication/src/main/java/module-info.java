module com.nextbreakpoint.shop.authentication {
    requires com.nextbreakpoint.shop.common;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.shop.authentication to com.nextbreakpoint.shop.common;
}