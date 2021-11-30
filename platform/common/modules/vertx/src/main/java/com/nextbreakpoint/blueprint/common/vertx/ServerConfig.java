package com.nextbreakpoint.blueprint.common.vertx;

import lombok.*;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerConfig {
    private final String jksStorePath;
    private final String jksStoreSecret;
}
