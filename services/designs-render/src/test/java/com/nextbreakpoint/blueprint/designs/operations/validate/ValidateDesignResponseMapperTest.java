package com.nextbreakpoint.blueprint.designs.operations.validate;

import com.nextbreakpoint.blueprint.common.core.ValidationStatus;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ValidateDesignResponseMapperTest {
    private final ValidateDesignResponseMapper mapper = new ValidateDesignResponseMapper();

    @ParameterizedTest
    @MethodSource("someFixtures")
    void shouldCreateResponse(ValidationStatus status, List<String> errors) {
        final var response = ValidateDesignResponse.builder()
                .withStatus(status)
                .withErrors(errors)
                .build();

        final var json = new JsonObject(mapper.transform(response));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(json.getString("status")).isEqualTo(status.name());
        softly.assertThat(json.getJsonArray("errors")).hasSize(errors.size());
        IntStream.range(0, errors.size())
                .forEach(index -> softly.assertThat(json.getJsonArray("errors").getString(index)).isEqualTo(errors.get(index)));
        softly.assertAll();
    }

    public static Stream<Arguments> someFixtures() {
        return Stream.of(
                Arguments.of(ValidationStatus.ACCEPTED, List.of()),
                Arguments.of(ValidationStatus.REJECTED, List.of("some error")),
                Arguments.of(ValidationStatus.REJECTED, List.of("some error", "some other error"))
        );
    }
}