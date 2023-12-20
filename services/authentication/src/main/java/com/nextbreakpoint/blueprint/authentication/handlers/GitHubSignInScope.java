package com.nextbreakpoint.blueprint.authentication.handlers;

import com.nextbreakpoint.blueprint.authentication.common.RoutingContextAdapter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.http.Cookie;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with", toBuilder = true)
public class GitHubSignInScope {
    private RoutingContextAdapter routingContext;
    private String oauthAccessToken;
    private String jwtAccessToken;
    private String redirectTo;
    private String userEmail;
    private JsonArray accounts;
    private JsonObject account;
    private Cookie cookie;
}
