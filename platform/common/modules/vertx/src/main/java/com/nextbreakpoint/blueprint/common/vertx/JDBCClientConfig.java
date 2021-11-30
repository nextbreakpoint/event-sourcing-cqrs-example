package com.nextbreakpoint.blueprint.common.vertx;

import lombok.*;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JDBCClientConfig {
    private final String url;
    private final String driver;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    private final int minPoolSize;
}
