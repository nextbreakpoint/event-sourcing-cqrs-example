package com.nextbreakpoint.blueprint.common.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentTest {
    @Test
    public void shouldResolveEnvironmentVariables() {
        Environment env = Environment.createEnvironment(String::toUpperCase, (name) -> null);
        assertThat(env.resolve("hello")).isEqualTo("hello");
        assertThat(env.resolve("$ {hello}")).isEqualTo("$ {hello}");
        assertThat(env.resolve("${hello}")).isEqualTo("HELLO");
        assertThat(env.resolve("$-${hello}-$")).isEqualTo("$-HELLO-$");
        assertThat(env.resolve("#${hello}_${world}!")).isEqualTo("#HELLO_WORLD!");
    }

    @Test
    public void shouldResolveSystemProperties() {
        Environment env = Environment.createEnvironment((name) -> null, String::toUpperCase);
        assertThat(env.resolve("hello")).isEqualTo("hello");
        assertThat(env.resolve("$ {hello}")).isEqualTo("$ {hello}");
        assertThat(env.resolve("${hello}")).isEqualTo("HELLO");
        assertThat(env.resolve("$-${hello}-$")).isEqualTo("$-HELLO-$");
        assertThat(env.resolve("#${hello}_${world}!")).isEqualTo("#HELLO_WORLD!");
    }
}
