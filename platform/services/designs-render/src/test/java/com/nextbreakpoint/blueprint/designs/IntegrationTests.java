package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.*;
import com.nextbreakpoint.blueprint.common.events.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.TileRenderRequestedOutputMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-render service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("IntegrationTests");

    @BeforeAll
    public static void before() {
        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @AfterEach
    public void reset() {
        RestAssured.reset();
    }

    @Test
    @DisplayName("Should start rendering an image after receiving a TileRenderRequested event")
    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage() {
        final UUID designId1 = UUID.randomUUID();
        final UUID designId2 = UUID.randomUUID();
        final UUID designId3 = UUID.randomUUID();
        final UUID designId4 = UUID.randomUUID();
        final UUID designId5 = UUID.randomUUID();

        final TileRenderRequested tileRenderRequested1 = new TileRenderRequested(designId1, TestConstants.REVISION_0, Checksum.of(TestConstants.JSON_1), TestConstants.JSON_1, 0, 0, 0);

        final OutputMessage tileRenderRequestedMessage1 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderRequested1, TestConstants.TRACING);

        final TileRenderRequested tileRenderRequested2 = new TileRenderRequested(designId2, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 4, 1, 2);

        final OutputMessage tileRenderRequestedMessage2 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderRequested2, TestConstants.TRACING);

        final TileRenderRequested tileRenderRequested3 = new TileRenderRequested(designId3, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 5, 1, 2);

        final OutputMessage tileRenderRequestedMessage3 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderRequested3, TestConstants.TRACING);

        final TileRenderRequested tileRenderRequested4 = new TileRenderRequested(designId4, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 6, 1, 2);

        final OutputMessage tileRenderRequestedMessage4 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderRequested4, TestConstants.TRACING);

        final TileRenderRequested tileRenderRequested5 = new TileRenderRequested(designId5, TestConstants.REVISION_1, Checksum.of(TestConstants.JSON_2), TestConstants.JSON_2, 7, 1, 2);

        final OutputMessage tileRenderRequestedMessage5 = new TileRenderRequestedOutputMapper(TestConstants.MESSAGE_SOURCE, TestUtils::createRenderKey).transform(tileRenderRequested5, TestConstants.TRACING);

        final List<OutputMessage> messages = List.of(tileRenderRequestedMessage1, tileRenderRequestedMessage2, tileRenderRequestedMessage3, tileRenderRequestedMessage4, tileRenderRequestedMessage5);

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(messages);
    }

    @Test
    @DisplayName("Should not return errors when design is valid")
    public void shouldReturnNoErrorsWhenDesignIsValid() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/validate"))
                .then().assertThat().statusCode(200)
                .and().assertThat().body("status", Matchers.equalTo(ValidationStatus.ACCEPTED.toString()))
                .and().assertThat().body("errors", Matchers.empty());
    }

    @Test
    @DisplayName("Should return errors when design is invalid")
    public void shouldReturnErrorsWhenDesignIsInvalid() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT_WITH_ERRORS))
                .when().post(testCases.makeBaseURL("/v1/designs/validate"))
                .then().assertThat().statusCode(200)
                .and().assertThat().body("status", Matchers.equalTo(ValidationStatus.REJECTED.toString()))
                .and().assertThat().body("errors", Matchers.equalTo(List.of("[1:0] Parse failed. Expected tokens: FRACTAL, 'fractal'")));
    }

    @Test
    @DisplayName("Should return the design when uploading a file")
    public void shouldReturnTheDesignWhenUploadingAFile() throws IOException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        final String manifest = getContent("/test.nf/manifest");
        final String metadata = getContent("/test.nf/metadata");
        final String script = getContent("/test.nf/script");

        try (InputStream inputStream = getClass().getResourceAsStream("/test.nf.zip")) {
            given().config(TestUtils.getRestAssuredConfig())
                    .with().header(AUTHORIZATION, authorization)
                    .and().multiPart("fileName", "test.nf.zip", inputStream)
                    .when().post(testCases.makeBaseURL("/v1/designs/upload"))
                    .then().assertThat().statusCode(200)
                    .and().assertThat().body("manifest", Matchers.equalTo(manifest))
                    .and().assertThat().body("metadata", Matchers.equalTo(metadata))
                    .and().assertThat().body("script", Matchers.equalTo(script));
        }
    }

    @Test
    @DisplayName("Should return file when design is valid")
    public void shouldReturnFileWhenDesignIsValid() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().accept("application/zip")
                .and().body(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/download"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/zip")
                .and().assertThat().content(Matchers.notNullValue());
    }

    private String getContent(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return IOUtils.toString(inputStream);
        }
    }
}