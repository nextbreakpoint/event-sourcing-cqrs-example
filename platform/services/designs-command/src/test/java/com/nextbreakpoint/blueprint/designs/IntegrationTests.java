package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of designs-command service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases("IntegrationTests");

    @BeforeAll
    public static void before() {
        System.setProperty("http.port", "30121");

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
    @DisplayName("Should forbid a POST request on /v1/designs without access token")
    public void shouldForbidPostRequestOnDesignsWithoutAccessToken() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT))
                .when().post(testCases.makeBaseURL("/v1/designs"))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a PUT request on /v1/designs/id without access token")
    public void shouldForbidPutRequestOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().contentType(ContentType.JSON)
                .and().accept(ContentType.JSON)
                .and().body(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT))
                .when().put(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid a DELETE request on /v1/designs/id without access token")
    public void shouldForbidDeleteRequestOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
        given().config(TestUtils.getRestAssuredConfig())
                .and().accept(ContentType.JSON)
                .when().delete(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
                .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should send a message after accepting a POST request on /v1/designs")
    public void shouldSendAMessageAfterAcceptingAPostRequestOnDesigns() throws IOException {
        testCases.shouldPublishDesignInsertRequestedEventWhenReceivingAInsertDesignRequest();
    }

    @Test
    @DisplayName("Should send a message after accepting a PUT request on /v1/designs/id")
    public void shouldSendAMessageAfterAcceptingAPutRequestOnDesigns() throws IOException {
        testCases.shouldPublishDesignUpdateRequestedEventWhenReceivingAUpdateDesignRequest();
    }

    @Test
    @DisplayName("Should send a message after accepting a DELETE request on /v1/designs/id")
    public void shouldSendAMessageAfterAcceptingADeleteRequestOnDesigns() throws IOException {
        testCases.shouldPublishDesignDeleteRequestedEventWhenReceivingADeleteDesignRequest();
    }
}
