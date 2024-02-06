package com.nextbreakpoint.blueprint.designs.controllers.get;

import io.vertx.rxjava.ext.web.RoutingContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetImageRequestMapperTest {
    private final RoutingContext context = mock();

    private final GetImageRequestMapper mapper = new GetImageRequestMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateRequest(String checksum) {
        givenARequest(checksum);

        final var request = mapper.transform(context);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(request.getChecksum()).isEqualTo(checksum);
        softly.assertAll();
    }

    @Test
    void shouldThrowExceptionWhenQueryParameterIsMissing() {
        givenARequest(null);

        assertThatThrownBy(() -> mapper.transform(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the required path parameter: checksum is missing");
    }

    private void givenARequest(String checksum) {
        when(context.pathParam("checksum")).thenReturn(checksum);
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of("123"),
                Arguments.of("456")
        );
    }
}