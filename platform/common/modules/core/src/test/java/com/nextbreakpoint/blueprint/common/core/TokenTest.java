package com.nextbreakpoint.blueprint.common.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenTest {
    @ParameterizedTest
    @MethodSource("generateArguments")
    void shouldCreateToken(long timestamp, long offset, String expectedToken) {
        assertThat(Token.from(timestamp, offset)).isEqualTo(expectedToken);
    }

    public static Stream<Arguments> generateArguments() {
        return Stream.of(
                Arguments.of(0L, 0L, "0000000000000000-0000000000000000"),
                Arguments.of(1L, 1L, "0000000000000001-0000000000000001"),
                Arguments.of(1L, 2L, "0000000000000001-0000000000000002"),
                Arguments.of(2L, 0L, "0000000000000002-0000000000000000")
        );
    }
}
