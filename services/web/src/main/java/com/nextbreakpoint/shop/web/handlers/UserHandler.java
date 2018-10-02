package com.nextbreakpoint.shop.web.handlers;

import com.nextbreakpoint.shop.common.vertx.Authentication;
import com.nextbreakpoint.shop.common.model.Authority;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.util.Arrays;

import static com.nextbreakpoint.shop.common.model.Authority.PLATFORM;
import static com.nextbreakpoint.shop.common.model.ContentType.APPLICATION_JSON;
import static com.nextbreakpoint.shop.common.model.Headers.ACCEPT;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static java.util.Arrays.asList;

public class UserHandler implements Handler<RoutingContext> {
    private final JWTAuth jwtProvider;
    private final WebClient accountsClient;

    public UserHandler(JWTAuth jwtProvider, WebClient accountsClient) {
        this.jwtProvider = jwtProvider;
        this.accountsClient = accountsClient;
    }

    public void handle(RoutingContext routingContext) {
        try {
            Authentication.getUser(jwtProvider, routingContext).subscribe(user -> handleUser(routingContext, user), err -> routingContext.next());
        } catch (Exception e) {
            // TODO log error
            routingContext.put("username", "Stranger");
            routingContext.next();
        }
    }

    private void handleUser(RoutingContext routingContext, User user) {
        try {
            if (isAnonymous(user)) {
                routingContext.put("username", "Stranger");
                routingContext.next();
            } else {
                fetchAccount(routingContext, user);
            }
        } catch (Exception e) {
            // TODO log error
            routingContext.put("username", "Stranger");
            routingContext.next();
        }
    }

    private void fetchAccount(RoutingContext routingContext, User user) {
        final String userUuid = user.principal().getString("user");

        final String token = Authentication.generateToken(jwtProvider, Authentication.NULL_USER_UUID, Arrays.asList(PLATFORM));

        accountsClient.get("/a/accounts/" + userUuid)
                .putHeader(AUTHORIZATION, Authentication.makeAuthorization(token))
                .putHeader(ACCEPT, APPLICATION_JSON)
                .rxSend().subscribe(response -> handleAccount(routingContext, user, response), err -> routingContext.next());
    }

    private void handleAccount(RoutingContext routingContext, User user, HttpResponse<Buffer> response) {
        if (response.statusCode() == 200) {
            final JsonObject account = response.bodyAsJsonObject();
            final String name = account.getString("name");
            final String role = account.getString("role");
            routingContext.put("username", name);
            routingContext.put("logged", true);
            routingContext.put("role", role);
            routingContext.setUser(user);
        } else {
            // TODO log error
            routingContext.put("username", "Stranger");
        }
        routingContext.next();
    }

    private boolean isAnonymous(User user) {
        return Authentication.hasRole(user, asList(Authority.ANONYMOUS)).isPresent();
    }

    public static UserHandler create(JWTAuth jwtProvider, WebClient accountsClient) {
        return new UserHandler(jwtProvider, accountsClient);
    }
}
