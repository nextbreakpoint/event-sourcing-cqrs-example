package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.Row;
import com.nextbreakpoint.blueprint.common.events.*;
import io.vertx.core.json.Json;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAssertions {
    private TestAssertions() {}
}
