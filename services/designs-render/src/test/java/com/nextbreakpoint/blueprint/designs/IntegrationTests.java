package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.ContentType;
import com.nextbreakpoint.blueprint.common.core.IOUtils;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.ValidationStatus;
import com.nextbreakpoint.blueprint.common.events.avro.TileRenderRequested;
import com.nextbreakpoint.blueprint.common.vertx.MessageFactory;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT_WITH_ERRORS;
import static io.restassured.RestAssured.given;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-render service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("DesignsRenderIntegrationTests");

    @BeforeAll
    public static void before() {
        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @BeforeEach
    public void beforeEach() {
        testCases.deleteData();
        testCases.getSteps().reset();
    }

    @AfterEach
    public void reset() {
        RestAssured.reset();
    }

    @Test
    @DisplayName("Should start rendering an image after receiving a TileRenderRequested message")
    public void shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage() {
        var tileRenderRequested1 = TileRenderRequested.newBuilder()
                .setDesignId(DESIGN_ID_1)
                .setCommandId(COMMAND_ID_1)
                .setData(DATA_1)
                .setChecksum(Checksum.of(DATA_1))
                .setRevision(REVISION_0)
                .setLevel(0)
                .setRow(0)
                .setCol(0)
                .build();

        var tileRenderRequested2 = TileRenderRequested.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setData(DATA_2)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setLevel(4)
                .setRow(1)
                .setCol(2)
                .build();

        var tileRenderRequested3 = TileRenderRequested.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setData(DATA_2)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setLevel(5)
                .setRow(1)
                .setCol(2)
                .build();

        var tileRenderRequested4 = TileRenderRequested.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setData(DATA_2)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setLevel(6)
                .setRow(1)
                .setCol(2)
                .build();

        var tileRenderRequested5 = TileRenderRequested.newBuilder()
                .setDesignId(DESIGN_ID_2)
                .setCommandId(COMMAND_ID_2)
                .setData(DATA_2)
                .setChecksum(Checksum.of(DATA_2))
                .setRevision(REVISION_1)
                .setLevel(7)
                .setRow(1)
                .setCol(2)
                .build();

        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage1 = MessageFactory.<TileRenderRequested>of(MESSAGE_SOURCE).createOutputMessage(tileRenderRequested1.getDesignId().toString(), tileRenderRequested1);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage2 = MessageFactory.<TileRenderRequested>of(MESSAGE_SOURCE).createOutputMessage(tileRenderRequested2.getDesignId().toString(), tileRenderRequested2);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage3 = MessageFactory.<TileRenderRequested>of(MESSAGE_SOURCE).createOutputMessage(tileRenderRequested3.getDesignId().toString(), tileRenderRequested3);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage4 = MessageFactory.<TileRenderRequested>of(MESSAGE_SOURCE).createOutputMessage(tileRenderRequested4.getDesignId().toString(), tileRenderRequested4);
        final OutputMessage<TileRenderRequested> tileRenderRequestedMessage5 = MessageFactory.<TileRenderRequested>of(MESSAGE_SOURCE).createOutputMessage(tileRenderRequested5.getDesignId().toString(), tileRenderRequested5);

        final List<OutputMessage<TileRenderRequested>> tileRenderRequestedMessages = List.of(
                tileRenderRequestedMessage1,
                tileRenderRequestedMessage2,
                tileRenderRequestedMessage3,
                tileRenderRequestedMessage4,
                tileRenderRequestedMessage5
        );

        testCases.shouldStartRenderingAnImageWhenReceivingATileRenderRequestedMessage(tileRenderRequestedMessages);
    }

    @Test
    @DisplayName("Should not return errors when design is valid")
    public void shouldReturnNoErrorsWhenDesignIsValid() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.APPLICATION_JSON)
                .and().accept(ContentType.APPLICATION_JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
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
                .and().contentType(ContentType.APPLICATION_JSON)
                .and().accept(ContentType.APPLICATION_JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT_WITH_ERRORS))
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
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/download"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/zip")
                .and().assertThat().body(Matchers.notNullValue());
    }

    private String getContent(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return IOUtils.toString(inputStream);
        }
    }
}