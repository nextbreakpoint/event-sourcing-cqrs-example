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
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.designs.TestConstants.CHECKSUM;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DATA_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.INVALID_MANIFEST;
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
    @DisplayName("Should allow a POST request on /v1/designs/validate and return status code 200 when design does not have validation errors")
    public void shouldAllowPostRequestOnDesignsSlashValidateAndReturnStatusCode200WhenDesignDoesNotHaveValidationErrors() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.APPLICATION_JSON)
                .and().accept(ContentType.APPLICATION_JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/validate"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/json")
                .and().assertThat().body("status", Matchers.equalTo(ValidationStatus.ACCEPTED.toString()))
                .and().assertThat().body("errors", Matchers.empty());
    }

    @Test
    @DisplayName("Should allow a POST request on /v1/designs/validate and return status code 200 when design has validation errors")
    public void shouldAllowPostRequestOnDesignsSlashValidateAndReturnStatusCode200WhenDesignHasValidationErrors() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.APPLICATION_JSON)
                .and().accept(ContentType.APPLICATION_JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT_WITH_ERRORS))
                .when().post(testCases.makeBaseURL("/v1/designs/validate"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/json")
                .and().assertThat().body("status", Matchers.equalTo(ValidationStatus.REJECTED.toString()))
                .and().assertThat().body("errors", Matchers.equalTo(List.of("[1:0] Parse failed. Expected tokens: FRACTAL, 'fractal'")));
    }

    @Test
    @DisplayName("Should forbid POST request on /v1/designs/validate when user is not authenticated")
    public void shouldForbidPostRequestOnDesignsSlashValidateWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType(ContentType.APPLICATION_JSON)
                .and().accept(ContentType.APPLICATION_JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/validate"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid POST request on /v1/designs/validate when user is not authorized")
    public void shouldForbidPostRequestOnDesignsSlashValidateWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.APPLICATION_JSON)
                .and().accept(ContentType.APPLICATION_JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/validate"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should allow a POST request on /v1/designs/upload and return status code 200 when design does not have errors")
    public void shouldAllowPostRequestOnDesignsSlashUploadAndReturnStatusCode200WhenDesignDoesNotHaveErrors() throws IOException {
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
                    .and().assertThat().contentType("application/json")
                    .and().assertThat().body("manifest", Matchers.equalTo(manifest))
                    .and().assertThat().body("metadata", Matchers.equalTo(metadata))
                    .and().assertThat().body("script", Matchers.equalTo(script));
        }
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs/upload when user is not authenticated")
    public void shouldForbidPostRequestOnDesignsSlashUploadWhenUserIsNotAuthenticated() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/test.nf.zip")) {
            given().config(TestUtils.getRestAssuredConfig())
                    .and().multiPart("fileName", "test.nf.zip", inputStream)
                    .when().post(testCases.makeBaseURL("/v1/designs/upload"))
                    .then().assertThat().statusCode(403);
        }
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs/upload when user is not authorized")
    public void shouldForbidPostRequestOnDesignsSlashUploadWhenUserIsNotAuthorized() throws IOException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        try (InputStream inputStream = getClass().getResourceAsStream("/test.nf.zip")) {
            given().config(TestUtils.getRestAssuredConfig())
                    .with().header(AUTHORIZATION, authorization)
                    .and().multiPart("fileName", "test.nf.zip", inputStream)
                    .when().post(testCases.makeBaseURL("/v1/designs/upload"))
                    .then().assertThat().statusCode(403);
        }
    }

    @Test
    @DisplayName("Should allow a POST request on /v1/designs/download and return status code 200 when design does not have errors")
    public void shouldAllowPostRequestOnDesignsSlashDownloadAndReturnStatusCode200WhenDesignDoesNotHaveErrors() throws MalformedURLException {
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

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs/download when user is not authenticated")
    public void shouldForbidPostRequestOnDesignsSlashDownloadWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType("application/json")
                .and().accept("application/zip")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/download"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs/download when user is not authorized")
    public void shouldForbidPostRequestOnDesignsSlashDownloadWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().accept("application/zip")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/download"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should allow a POST request on /v1/designs/render and return status code 200 when design does not have errors")
    public void shouldAllowPostRequestOnDesignsSlashRenderAndReturnStatusCode200WhenDesignDoesNotHaveErrors() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/render"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/json")
                .and().assertThat().body("checksum", Matchers.equalTo(CHECKSUM))
                .and().assertThat().body("errors", Matchers.empty());
    }

    @Test
    @DisplayName("Should allow a POST request on /v1/designs/render and return status code 200 when script has errors")
    public void shouldAllowPostRequestOnDesignsSlashRenderAndReturnStatusCode200WhenScriptHasErrors() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT_WITH_ERRORS))
                .when().post(testCases.makeBaseURL("/v1/designs/render"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/json")
                .and().assertThat().body("checksum", Matchers.notNullValue())
                .and().assertThat().body("errors", Matchers.empty());
    }

    @Test
    @DisplayName("Should allow a POST request on /v1/designs/render and return status code 200 when manifest is invalid")
    public void shouldAllowPostRequestOnDesignsSlashRenderAndReturnStatusCode200WhenManifestIsInvalid() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().body(TestUtils.createPostData(INVALID_MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/render"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/json")
                .and().assertThat().body("checksum", Matchers.nullValue())
                .and().assertThat().body("errors", Matchers.hasSize(1));
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs/render when user is not authenticated")
    public void shouldForbidPostRequestOnDesignsSlashRenderWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType("application/json")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/render"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs/render when user is not authorized")
    public void shouldForbidPostRequestOnDesignsSlashRenderWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/render"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should allow a GET request on /v1/designs/image and return status code 200 when image exists")
    public void shouldAllowGetRequestOnDesignsSlashImageAndReturnStatusCode200WhenImageExists() throws IOException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType("application/json")
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs/render"))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("application/json")
                .and().assertThat().body("checksum", Matchers.equalTo(CHECKSUM))
                .and().assertThat().body("errors", Matchers.empty());

        byte[] result = given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().accept("image/png")
                .when().get(testCases.makeBaseURL("/v1/designs/image/" + CHECKSUM))
                .then().assertThat().statusCode(200)
                .and().assertThat().contentType("image/png")
                .and().extract().asByteArray();

        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(image.getWidth()).isEqualTo(512);
        softly.assertThat(image.getHeight()).isEqualTo(512);
        softly.assertAll();
    }

    @Test
    @DisplayName("Should allow a GET request on /v1/designs/image and return status code 404 when image does not exist")
    public void shouldAllowGetRequestOnDesignsSlashImageAndReturnStatusCode404WhenImageDoesNotExist() throws IOException {
        final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().accept("image/png")
                .and().queryParam("checksum", "123")
                .when().get(testCases.makeBaseURL("/v1/designs/image/123"))
                .then().assertThat().statusCode(404);
    }

    @Test
    @DisplayName("Should forbid a GET request on /v1/designs/image when user is not authenticated")
    public void shouldForbidGetRequestOnDesignsSlashImageWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().accept("image/png")
                .when().get(testCases.makeBaseURL("/v1/designs/image/" + CHECKSUM))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a GET request on /v1/designs/image when user is not authorized")
    public void shouldForbidGetRequestOnDesignsSlashImageWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().accept("image/png")
                .when().get(testCases.makeBaseURL("/v1/designs/image/" + CHECKSUM))
                .then().assertThat().statusCode(403);
    }

    private String getContent(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return IOUtils.toString(inputStream);
        }
    }
}