package com.nextbreakpoint.blueprint.authentication.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.auth.oauth2.OAuth2Auth;

import java.util.Objects;

public class OAuthAdapter {
    private final OAuth2Auth oauthHandler;
    private final String oauthAuthority;
    private final String callbackPath;
    private final String authUrl;

    public OAuthAdapter(OAuth2Auth oauthHandler, String authUrl, String oauthAuthority, String callbackPath) {
        this.oauthHandler = Objects.requireNonNull(oauthHandler);
        this.oauthAuthority = Objects.requireNonNull(oauthAuthority);
        this.callbackPath = Objects.requireNonNull(callbackPath);
        this.authUrl = Objects.requireNonNull(authUrl);
    }

    public String authorizeURL(String uri) {
        return oauthHandler.authorizeURL(newAuthorizationURL(uri));
    }

    public void authenticate(String code, Handler<AsyncResult<User>> handler) {
        oauthHandler.authenticate(getCredentials(code), handler);
    }

    private OAuth2AuthorizationURL newAuthorizationURL(String uri) {
        return new OAuth2AuthorizationURL(new JsonObject()
                .put("redirect_uri", authUrl + callbackPath)
                .put("scope", oauthAuthority)
                .put("state", uri)
        );
    }

    private Oauth2Credentials getCredentials(String code) {
        return new Oauth2Credentials()
                .setRedirectUri(authUrl + callbackPath)
                .setFlow(OAuth2FlowType.AUTH_CODE)
                .setCode(code);
    }
}
