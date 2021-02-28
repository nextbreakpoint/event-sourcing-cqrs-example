package com.nextbreakpoint.blueprint.common.core;

import com.nextbreakpoint.blueprint.common.core.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentTest {
    @Test
    public void shouldResolveEnvironmentVariables() {
        Environment env = Environment.createEnvironment((name) -> "hello", (name) -> null);
        assertThat(env.resolve("test")).isEqualTo("test");
        assertThat(env.resolve("$ {test}")).isEqualTo("$ {test}");
        assertThat(env.resolve("${test}")).isEqualTo("hello");
    }

    @Test
    public void shouldResolveSystemProperties() {
        Environment env = Environment.createEnvironment((name) -> null, (name) -> "world");
        assertThat(env.resolve("test")).isEqualTo("test");
        assertThat(env.resolve("$ {test}")).isEqualTo("$ {test}");
        assertThat(env.resolve("${test}")).isEqualTo("world");
    }
}
