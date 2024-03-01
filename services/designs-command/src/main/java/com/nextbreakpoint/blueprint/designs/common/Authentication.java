package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.common.core.Headers;
import io.vertx.rxjava.core.http.Cookie;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Authentication {
    private Authentication() {}

    public static String getToken(RoutingContext routingContext) {
        final String authorization = routingContext.request().getHeader(Headers.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            log.debug("Authorisation header is present");
            return authorization.substring("Bearer ".length());
        } else {
            final Cookie cookie = routingContext.getCookie("token");
            if (cookie != null && !cookie.getValue().isEmpty()) {
                log.debug("Token cookie is present");
                return cookie.getValue();
            }
        }
        log.debug("Authorisation header and token cookie not present");
        return null;
    }
}
