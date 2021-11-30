package com.nextbreakpoint.blueprint.common.vertx;

import lombok.*;

@Data
@Builder(access = AccessLevel.PUBLIC, setterPrefix = "with")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CassandraClientConfig {
    private final String clusterName;
    private final String keyspace;
    private final String username;
    private final String password;
    private final String[] contactPoints;
    private final int port;
}
