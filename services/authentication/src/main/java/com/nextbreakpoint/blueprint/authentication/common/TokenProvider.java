package com.nextbreakpoint.blueprint.authentication.common;

import com.nextbreakpoint.blueprint.common.vertx.Authentication;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import rx.Single;

import java.util.List;
import java.util.Objects;

public class TokenProvider {
    private final JWTAuth jwtProvider;

    public TokenProvider(JWTAuth jwtProvider) {
        this.jwtProvider = Objects.requireNonNull(jwtProvider);
    }

    public Single<String> generateToken(String userUuid, List<String> authorities) {
        return Single.fromCallable(() -> Authentication.generateToken(jwtProvider, userUuid, authorities));
    }
}
