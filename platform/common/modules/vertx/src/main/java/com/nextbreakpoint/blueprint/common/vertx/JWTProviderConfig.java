package com.nextbreakpoint.blueprint.common.vertx;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JWTProviderConfig {
    private final String keyStoreType;
    private final String keyStorePath;
    private final String keyStoreSecret;
}
