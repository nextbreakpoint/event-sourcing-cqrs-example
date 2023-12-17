package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import io.vertx.rxjava.core.http.Cookie;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with", toBuilder = true)
public class GitHubSignOutScope {
    private RoutingContextAdapter routingContext;
    private String redirectTo;
    private Cookie cookie;
}
