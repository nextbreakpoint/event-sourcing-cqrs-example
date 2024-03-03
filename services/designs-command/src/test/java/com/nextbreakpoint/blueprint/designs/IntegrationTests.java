package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.core.Authority;
import com.xebialabs.restito.server.StubServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.common.core.Headers.CONTENT_TYPE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.*;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_DELETE_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_INSERT_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_UPDATE_COMMAND;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.header;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static com.xebialabs.restito.semantics.Condition.withPostBody;
import static io.restassured.RestAssured.given;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-command service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("DesignsCommandIntegrationTests");

    private static final StubServer designsRenderStub = new StubServer(Integer.parseInt("39001"));

    @BeforeAll
    public static void before() {
        System.setProperty("http.port", "30121");

        if (designsRenderStub != null) {
            designsRenderStub.start();
        }

        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();

        if (designsRenderStub != null) {
            designsRenderStub.stop();
        }
    }

    @BeforeEach
    public void reset() {
        RestAssured.reset();

        if (designsRenderStub != null) {
            designsRenderStub.clear();
        }

        testCases.deleteData();
        testCases.getSteps().reset();
    }

    @Test
    @DisplayName("Should allow a OPTIONS request on /v1/designs without access token")
    public void shouldAllowOptionsRequestOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .with().header("Origin", testCases.getOriginUrl())
                .when().options(testCases.makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(204)
                .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
                .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should allow a OPTIONS request on /v1/designs/id without access token")
    public void shouldAllowOptionsRequestOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .with().header("Origin", testCases.getOriginUrl())
                .when().options(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(204)
                .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
                .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs when user is not authenticated")
    public void shouldForbidPostRequestOnDesignsWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a PUT request on /v1/designs/id when user is not authenticated")
    public void shouldForbidPutRequestOnDesignsSlashIdWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().put(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a DELETE request on /v1/designs/id when user is not authenticated")
    public void shouldForbidDeleteRequestOnDesignsSlashIdWhenUserIsNotAuthenticated() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().accept(ContentType.JSON)
                .when().delete(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a POST request on /v1/designs when user is not authorized")
    public void shouldForbidPostRequestOnDesignsWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a PUT request on /v1/designs/id when user is not authorized")
    public void shouldForbidPutRequestOnDesignsSlashIdWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT))
                .when().put(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a DELETE request on /v1/designs/id when user is not authorized")
    public void shouldForbidDeleteRequestOnDesignsSlashIdWhenUserIsNotAuthorized() throws MalformedURLException {
        final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

        given().config(TestUtils.getRestAssuredConfig())
                .with().header(AUTHORIZATION, authorization)
                .and().accept(ContentType.JSON)
                .when().delete(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should send a message after accepting a POST request on /v1/designs")
    public void shouldSendAMessageAfterAcceptingAPostRequestOnDesigns() throws IOException {
        whenHttp(designsRenderStub)
                .match(post(TestConstants.DESIGNS_RENDER_VALIDATE_PATH), withHeader("Authorization"), withPostBody())
                .then(status(HttpStatus.OK_200), header(CONTENT_TYPE, "application/json"), stringContent("{\"status\":\"ACCEPTED\",\"errors\":[]}"));

        testCases.getSteps()
                .given().theUserId(USER_ID_1)
                .and().theManifest(MANIFEST)
                .and().theMetadata(METADATA)
                .and().theScript(SCRIPT)
                .and().anAuthorization(Authority.ADMIN)
                .when().discardReceivedCommands()
                .and().discardReceivedEvents()
                .and().submitInsertDesignRequest()
                .then().requestIsAccepted()
                .and().responseContainsDesignId()
                .and().aCommandMessageShouldBePublished(DESIGN_INSERT_COMMAND)
                .and().aDesignInsertCommandMessageShouldBeSaved()
                .and().aDesignInsertRequestedMessageShouldBePublished()
                .and().theDesignInsertRequestedEventShouldHaveExpectedValues();
    }

    @Test
    @DisplayName("Should send a message after accepting a PUT request on /v1/designs/id")
    public void shouldSendAMessageAfterAcceptingAPutRequestOnDesigns() throws IOException {
        whenHttp(designsRenderStub)
                .match(post(TestConstants.DESIGNS_RENDER_VALIDATE_PATH), withHeader("Authorization"), withPostBody())
                .then(status(HttpStatus.OK_200), header(CONTENT_TYPE, "application/json"), stringContent("{\"status\":\"ACCEPTED\",\"errors\":[]}"));

        testCases.getSteps()
                .given().theUserId(USER_ID_1)
                .and().theDesignId(DESIGN_ID_1)
                .and().theManifest(MANIFEST)
                .and().theMetadata(METADATA)
                .and().theScript(SCRIPT)
                .and().anAuthorization(Authority.ADMIN)
                .when().discardReceivedCommands()
                .and().discardReceivedEvents()
                .and().submitUpdateDesignRequest()
                .then().requestIsAccepted()
                .and().aCommandMessageShouldBePublished(DESIGN_UPDATE_COMMAND)
                .and().aDesignUpdateCommandMessageShouldBeSaved()
                .and().aDesignUpdateRequestedMessageShouldBePublished()
                .and().theDesignUpdateRequestedEventShouldHaveExpectedValues();
    }

    @Test
    @DisplayName("Should send a message after accepting a DELETE request on /v1/designs/id")
    public void shouldSendAMessageAfterAcceptingADeleteRequestOnDesigns() throws IOException {
        testCases.getSteps()
                .given().theUserId(USER_ID_2)
                .and().theDesignId(DESIGN_ID_2)
                .and().anAuthorization(Authority.ADMIN)
                .when().discardReceivedCommands()
                .and().discardReceivedEvents()
                .and().submitDeleteDesignRequest()
                .then().requestIsAccepted()
                .and().aCommandMessageShouldBePublished(DESIGN_DELETE_COMMAND)
                .and().aDesignDeleteCommandMessageShouldBeSaved()
                .and().aDesignDeleteRequestedMessageShouldBePublished()
                .and().theDesignDeleteRequestedEventShouldHaveExpectedValues();
    }

    @Test
    @DisplayName("Should not send a message after rejecting a POST request on /v1/designs")
    public void shouldNotSendAMessageAfterRejectingAPostRequestOnDesigns() throws IOException {
        whenHttp(designsRenderStub)
                .match(post(TestConstants.DESIGNS_RENDER_VALIDATE_PATH), withHeader("Authorization"), withPostBody())
                .then(status(HttpStatus.OK_200), header(CONTENT_TYPE, "application/json"), stringContent("{\"status\":\"REJECTED\",\"errors\":[\"some error\"]}"));

        testCases.getSteps()
                .given().theUserId(USER_ID_1)
                .and().theManifest(MANIFEST)
                .and().theMetadata(METADATA)
                .and().theScript(SCRIPT)
                .and().anAuthorization(Authority.ADMIN)
                .when().discardReceivedCommands()
                .and().discardReceivedEvents()
                .and().submitInsertDesignRequest()
                .then().requestIsRejected();
    }

    @Test
    @DisplayName("Should not send a message after rejecting a PUT request on /v1/designs/id")
    public void shouldNotSendAMessageAfterRejectingAPutRequestOnDesigns() throws IOException {
        whenHttp(designsRenderStub)
                .match(post(TestConstants.DESIGNS_RENDER_VALIDATE_PATH), withHeader("Authorization"), withPostBody())
                .then(status(HttpStatus.OK_200), header(CONTENT_TYPE, "application/json"), stringContent("{\"status\":\"REJECTED\",\"errors\":[\"some error\"]}"));

        testCases.getSteps()
                .given().theUserId(USER_ID_1)
                .and().theDesignId(DESIGN_ID_1)
                .and().theManifest(MANIFEST)
                .and().theMetadata(METADATA)
                .and().theScript(SCRIPT)
                .and().anAuthorization(Authority.ADMIN)
                .when().discardReceivedCommands()
                .and().discardReceivedEvents()
                .and().submitUpdateDesignRequest()
                .then().requestIsRejected();
    }
}
