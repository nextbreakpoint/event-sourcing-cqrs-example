package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of designs-aggregate-fetcher service")
public class IntegrationTests {
  private static final TestCases testCases = new TestCases();

  private static final String JSON_1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
  private static final String JSON_2 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT2)).toString();
  private static final String JSON_3 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT3)).toString();
  private static final String JSON_4 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT4)).toString();

  @BeforeAll
  public static void before() {
    testCases.before();
  }

  @AfterAll
  public static void after() {
    testCases.after();
  }

  @BeforeEach
  public void setup() {
    final Design design1 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_1, 0, JSON_1, Checksum.of(JSON_1), "CREATED", TestConstants.LEVELS, new ArrayList<>(), new Date());
    final Design design2 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_2, 1, JSON_2, Checksum.of(JSON_2), "UPDATED", TestConstants.LEVELS, new ArrayList<>(), new Date());
    final Design design3 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_3, 2, JSON_3, Checksum.of(JSON_3), "UPDATED", TestConstants.LEVELS, new ArrayList<>(), new Date());
    final Design design4 = new Design(Uuids.timeBased(), TestConstants.DESIGN_UUID_4, 4, JSON_4, Checksum.of(JSON_4), "DELETED", TestConstants.LEVELS, new ArrayList<>(), new Date());

    testCases.deleteDesigns();

    List.of(design1, design2, design3, design4).forEach(testCases::insertDesign);
  }

  @AfterEach
  public void reset() {
    RestAssured.reset();
  }

  @Test
  @DisplayName("Should allow OPTIONS on /designs without access token")
  public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().options(testCases.makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should allow OPTIONS on /v1/designs/id without access token")
  public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().options(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should forbid GET on /v1/designs when user has unknown authority")
  public void shouldForbidGetOnDesignsWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /v1/designs/id when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_1))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /v1/designs/designId/level/col/row/256.png when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_2 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is anonymous")
  public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    Document[] results = listDesigns(authorization);

    List<Document> sortedResults = Stream.of(results)
            .sorted(Comparator.comparing(Document::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
    assertThat(sortedResults.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2.toString());
    assertThat(sortedResults.get(2).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3.toString());
    assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_1));
    assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults.get(0).getModified()).isNotNull();
    assertThat(sortedResults.get(1).getModified()).isNotNull();
    assertThat(sortedResults.get(2).getModified()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    Document result = loadDesign(authorization, TestConstants.DESIGN_UUID_1);

    String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    assertThat(result.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
    assertThat(result.getJson()).isEqualTo(json1);
    assertThat(result.getModified()).isNotNull();
    assertThat(result.getChecksum()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    byte[] result = getTile(authorization, TestConstants.DESIGN_UUID_1);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is admin")
  public void shouldAllowGetOnDesignsWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);;

    Document[] results = listDesigns(authorization);

    List<Document> sortedResults = Stream.of(results)
            .sorted(Comparator.comparing(Document::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
    assertThat(sortedResults.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2.toString());
    assertThat(sortedResults.get(2).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3.toString());
    assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_1));
    assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults.get(0).getModified()).isNotNull();
    assertThat(sortedResults.get(1).getModified()).isNotNull();
    assertThat(sortedResults.get(2).getModified()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is admin")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);;

    Document result = loadDesign(authorization, TestConstants.DESIGN_UUID_1);

    String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    assertThat(result.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
    assertThat(result.getJson()).isEqualTo(json1);
    assertThat(result.getModified()).isNotNull();
    assertThat(result.getChecksum()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is admin")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);;

    byte[] result = getTile(authorization, TestConstants.DESIGN_UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is guest")
  public void shouldAllowGetOnDesignsWhenUserIsGuest() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);;

    Document[] results = listDesigns(authorization);

    List<Document> sortedResults = Stream.of(results)
            .sorted(Comparator.comparing(Document::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
    assertThat(sortedResults.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2.toString());
    assertThat(sortedResults.get(2).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3.toString());
    assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_1));
    assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults.get(0).getModified()).isNotNull();
    assertThat(sortedResults.get(1).getModified()).isNotNull();
    assertThat(sortedResults.get(2).getModified()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is guest")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);;

    Document result = loadDesign(authorization, TestConstants.DESIGN_UUID_1);

    String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    assertThat(result.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
    assertThat(result.getJson()).isEqualTo(json1);
    assertThat(result.getModified()).isNotNull();
    assertThat(result.getChecksum()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is guest")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);;

    byte[] result = getTile(authorization, TestConstants.DESIGN_UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/id does not exist")
  public void shouldReturnNotFoundWhenDeisgnDoesNotExist() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_0))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/designId/level/col/row/256.png does not exist")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeDoesNotExist() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_0 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/id has been deleted")
  public void shouldReturnNotFoundWhenDeisgnHasBeenDeleted() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_4))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/designId/level/col/row/256.png has been deleted")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeHasBeenDeleted() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);;

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_4 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(404);
  }

  private static Document[] listDesigns(String authorization) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(200)
            .extract().body().as(Document[].class);
  }

  private static Document loadDesign(String authorization, UUID uuid) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .extract().body().as(Document.class);
  }

  private static byte[] getTile(String authorization, UUID uuid) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().assertThat().contentType("image/png")
            .extract().response().asByteArray();
  }
}
