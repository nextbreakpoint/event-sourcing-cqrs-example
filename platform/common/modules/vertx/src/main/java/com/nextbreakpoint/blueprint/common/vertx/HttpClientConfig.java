package com.nextbreakpoint.blueprint.common.vertx;

import lombok.*;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpClientConfig {
    private final Boolean keepAlive;
    private final Boolean verifyHost;
    private final String keyStorePath;
    private final String keyStoreSecret;
    private final String trustStorePath;
    private final String trustStoreSecret;
}
