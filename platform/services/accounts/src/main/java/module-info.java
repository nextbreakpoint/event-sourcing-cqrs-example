module com.nextbreakpoint.shop.accounts {
    requires com.nextbreakpoint.shop.common;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    opens com.nextbreakpoint.shop.accounts to com.nextbreakpoint.shop.common;
}