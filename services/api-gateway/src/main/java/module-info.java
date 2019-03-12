module com.nextbreakpoint.shop.api.gateway {
    requires com.nextbreakpoint.shop.common;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.shop.gateway to com.nextbreakpoint.shop.common;
}