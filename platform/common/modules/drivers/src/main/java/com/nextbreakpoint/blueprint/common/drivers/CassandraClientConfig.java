package com.nextbreakpoint.blueprint.common.drivers;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

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
