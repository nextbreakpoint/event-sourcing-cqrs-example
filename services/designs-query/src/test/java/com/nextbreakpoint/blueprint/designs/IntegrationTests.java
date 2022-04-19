package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Checksum;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.core.Tiles;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentDeleteRequested;
import com.nextbreakpoint.blueprint.common.events.DesignDocumentUpdateRequested;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentDeleteRequestedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignDocumentUpdateRequestedOutputMapper;
import com.nextbreakpoint.blueprint.designs.model.Design;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.designs.model.DesignDocuments;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-query service")
public class IntegrationTests {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

  private static final TestCases testCases = new TestCases("IntegrationTests");

  private static final String JSON_1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
  private static final String JSON_2 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT2)).toString();
  private static final String JSON_3 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT3)).toString();

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
    final Design design1 = new Design(TestConstants.DESIGN_UUID_1, TestConstants.USER_ID, UUID.randomUUID(), JSON_1, Checksum.of(JSON_1), TestConstants.REVISION_0, "CREATED", false, TestConstants.LEVELS, TestUtils.getTiles(TestConstants.LEVELS, 0.0f), FORMATTER.format(Instant.now()), FORMATTER.format(Instant.now()));
    final Design design2 = new Design(TestConstants.DESIGN_UUID_2, TestConstants.USER_ID, UUID.randomUUID(), JSON_2, Checksum.of(JSON_2), TestConstants.REVISION_0, "UPDATED", false, TestConstants.LEVELS, TestUtils.getTiles(TestConstants.LEVELS, 0.2f), FORMATTER.format(Instant.now()), FORMATTER.format(Instant.now()));
    final Design design3 = new Design(TestConstants.DESIGN_UUID_3, TestConstants.USER_ID, UUID.randomUUID(), JSON_3, Checksum.of(JSON_3), TestConstants.REVISION_0, "UPDATED", false, TestConstants.LEVELS, TestUtils.getTiles(TestConstants.LEVELS, 0.5f), FORMATTER.format(Instant.now()), FORMATTER.format(Instant.now()));

    testCases.deleteDesigns();
    testCases.deleteDraftDesigns();

    List.of(design2, design3).forEach(testCases::insertDesign);
    List.of(design1, design2, design3).forEach(testCases::insertDraftDesign);
  }

  @AfterEach
  public void reset() {
    RestAssured.reset();
  }

  @Test
  @DisplayName("Should update the design after receiving a DesignDocumentUpdateRequested event")
  public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequested() {
    final UUID designId1 = UUID.randomUUID();
    final UUID designId2 = UUID.randomUUID();

    final List<Tiles> tiles1 = TestUtils.getTiles(TestConstants.LEVELS, 0f);
    final List<Tiles> tiles2 = TestUtils.getTiles(TestConstants.LEVELS, 50f);
    final List<Tiles> tiles3 = TestUtils.getTiles(TestConstants.LEVELS, 100f);
    final List<Tiles> tiles4 = TestUtils.getTiles(TestConstants.LEVELS, 100f);

    final DesignDocumentUpdateRequested designDocumentUpdateRequested1 = new DesignDocumentUpdateRequested(designId1, UUID.randomUUID(), TestConstants.USER_ID, TestConstants.REVISION_0, TestConstants.CHECKSUM_1, TestConstants.JSON_1, "CREATED", false, TestConstants.LEVELS, tiles1, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));
    final DesignDocumentUpdateRequested designDocumentUpdateRequested2 = new DesignDocumentUpdateRequested(designId1, UUID.randomUUID(), TestConstants.USER_ID, TestConstants.REVISION_1, TestConstants.CHECKSUM_1, TestConstants.JSON_1, "UPDATED", false, TestConstants.LEVELS, tiles2, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));
    final DesignDocumentUpdateRequested designDocumentUpdateRequested3 = new DesignDocumentUpdateRequested(designId1, UUID.randomUUID(), TestConstants.USER_ID, TestConstants.REVISION_2, TestConstants.CHECKSUM_1, TestConstants.JSON_1, "UPDATED", false, TestConstants.LEVELS, tiles3, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));
    final DesignDocumentUpdateRequested designDocumentUpdateRequested4 = new DesignDocumentUpdateRequested(designId2, UUID.randomUUID(), TestConstants.USER_ID, TestConstants.REVISION_0, TestConstants.CHECKSUM_2, TestConstants.JSON_2, "UPDATED", false, TestConstants.LEVELS, tiles4, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));

    final DesignDocumentUpdateRequestedOutputMapper outputMapper = new DesignDocumentUpdateRequestedOutputMapper(TestConstants.MESSAGE_SOURCE);

    final OutputMessage designDocumentUpdateRequestedMessage1 = outputMapper.transform(designDocumentUpdateRequested1);
    final OutputMessage designDocumentUpdateRequestedMessage2 = outputMapper.transform(designDocumentUpdateRequested2);
    final OutputMessage designDocumentUpdateRequestedMessage3 = outputMapper.transform(designDocumentUpdateRequested3);
    final OutputMessage designDocumentUpdateRequestedMessage4 = outputMapper.transform(designDocumentUpdateRequested4);

    final List<OutputMessage> outputMessages = List.of(designDocumentUpdateRequestedMessage1, designDocumentUpdateRequestedMessage2, designDocumentUpdateRequestedMessage3, designDocumentUpdateRequestedMessage4);

    testCases.shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequestedMessage(outputMessages);
  }

  @Test
  @DisplayName("Should delete the design after receiving a DesignDocumentDeleteRequested event")
  public void shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequested() {
    final UUID designId = UUID.randomUUID();

    final List<Tiles> tiles = TestUtils.getTiles(TestConstants.LEVELS, 100.0f);

    final DesignDocumentUpdateRequested designDocumentUpdateRequested = new DesignDocumentUpdateRequested(designId, UUID.randomUUID(), TestConstants.USER_ID, TestConstants.REVISION_0, TestConstants.CHECKSUM_2, TestConstants.JSON_2, "CREATED", false, TestConstants.LEVELS, tiles, LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")), LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));
    final DesignDocumentDeleteRequested designDocumentDeleteRequested = new DesignDocumentDeleteRequested(designId, UUID.randomUUID(), TestConstants.REVISION_0);

    final DesignDocumentUpdateRequestedOutputMapper outputMapper1 = new DesignDocumentUpdateRequestedOutputMapper(TestConstants.MESSAGE_SOURCE);
    final DesignDocumentDeleteRequestedOutputMapper outputMapper2 = new DesignDocumentDeleteRequestedOutputMapper(TestConstants.MESSAGE_SOURCE);

    final OutputMessage designDocumentUpdateRequestedMessage = outputMapper1.transform(designDocumentUpdateRequested);
    final OutputMessage designDocumentDeleteRequestedMessage = outputMapper2.transform(designDocumentDeleteRequested);

    testCases.shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequestedMessage(designDocumentUpdateRequestedMessage, designDocumentDeleteRequestedMessage);
  }

  @Test
  @DisplayName("Should allow OPTIONS on /designs without access token")
  public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().options(testCases.makeBaseURL("/v1/designs?draft=true"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", testCases.getOriginUrl())
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should allow OPTIONS on /v1/designs/id without access token")
  public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
    given().config(TestUtils.getRestAssuredConfig())
            .with().header("Origin", testCases.getOriginUrl())
            .when().options(testCases.makeBaseURL("/v1/designs/" + UUID.randomUUID() + "?draft=true"))
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
            .when().get(testCases.makeBaseURL("/v1/designs?draft=true"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /v1/designs/id when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_1 + "?draft=true"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /v1/designs/designId/level/col/row/256.png when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_2 + "/0/0/0/256.png?draft=true"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is anonymous")
  public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    DesignDocuments results = listDesigns(authorization);

    List<DesignDocument> sortedResults = results.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults).hasSize(3);

    assertThat(sortedResults.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1);
    assertThat(sortedResults.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2);
    assertThat(sortedResults.get(2).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3);
    assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_1));
    assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults.get(0).getRevision()).isNotNull();
    assertThat(sortedResults.get(1).getRevision()).isNotNull();
    assertThat(sortedResults.get(2).getRevision()).isNotNull();
    assertThat(sortedResults.get(0).getCreated()).isNotNull();
    assertThat(sortedResults.get(1).getCreated()).isNotNull();
    assertThat(sortedResults.get(2).getCreated()).isNotNull();
    assertThat(sortedResults.get(0).getUpdated()).isNotNull();
    assertThat(sortedResults.get(1).getUpdated()).isNotNull();
    assertThat(sortedResults.get(2).getUpdated()).isNotNull();
    assertThat(sortedResults.get(0).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults.get(1).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults.get(2).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults.get(0).getTiles()).isNotNull();
    assertThat(sortedResults.get(1).getTiles()).isNotNull();
    assertThat(sortedResults.get(2).getTiles()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    DesignDocument result = loadDesign(authorization, TestConstants.DESIGN_UUID_1);

    String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    assertThat(result.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1);
    assertThat(result.getJson()).isEqualTo(json1);
    assertThat(result.getCreated()).isNotNull();
    assertThat(result.getUpdated()).isNotNull();
    assertThat(result.getChecksum()).isNotNull();
    assertThat(result.getRevision()).isNotNull();
    assertThat(result.getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(result.getTiles()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    byte[] result = getTile(authorization, TestConstants.DESIGN_UUID_1);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is admin")
  public void shouldAllowGetOnDesignsWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    DesignDocuments results = listDesigns(authorization);

    List<DesignDocument> sortedResults = results.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults).hasSize(3);

    assertThat(sortedResults.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1);
    assertThat(sortedResults.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2);
    assertThat(sortedResults.get(2).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3);
    assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_1));
    assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults.get(0).getRevision()).isNotNull();
    assertThat(sortedResults.get(1).getRevision()).isNotNull();
    assertThat(sortedResults.get(2).getRevision()).isNotNull();
    assertThat(sortedResults.get(0).getCreated()).isNotNull();
    assertThat(sortedResults.get(1).getCreated()).isNotNull();
    assertThat(sortedResults.get(2).getCreated()).isNotNull();
    assertThat(sortedResults.get(0).getUpdated()).isNotNull();
    assertThat(sortedResults.get(1).getUpdated()).isNotNull();
    assertThat(sortedResults.get(2).getUpdated()).isNotNull();
    assertThat(sortedResults.get(0).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults.get(1).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults.get(2).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults.get(0).getTiles()).isNotNull();
    assertThat(sortedResults.get(1).getTiles()).isNotNull();
    assertThat(sortedResults.get(2).getTiles()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is admin")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    DesignDocument result = loadDesign(authorization, TestConstants.DESIGN_UUID_1);

    String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    assertThat(result.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1);
    assertThat(result.getJson()).isEqualTo(json1);
    assertThat(result.getCreated()).isNotNull();
    assertThat(result.getUpdated()).isNotNull();
    assertThat(result.getChecksum()).isNotNull();
    assertThat(result.getRevision()).isNotNull();
    assertThat(result.getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(result.getTiles()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is admin")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    byte[] result = getTile(authorization, TestConstants.DESIGN_UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is guest")
  public void shouldAllowGetOnDesignsWhenUserIsGuest() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

    DesignDocuments results1 = listDesigns(authorization, true);

    List<DesignDocument> sortedResults1 = results1.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults1).hasSize(3);

    assertThat(sortedResults1.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1);
    assertThat(sortedResults1.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2);
    assertThat(sortedResults1.get(2).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3);
    assertThat(sortedResults1.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_1));
    assertThat(sortedResults1.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults1.get(2).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults1.get(0).getRevision()).isNotNull();
    assertThat(sortedResults1.get(1).getRevision()).isNotNull();
    assertThat(sortedResults1.get(2).getRevision()).isNotNull();
    assertThat(sortedResults1.get(0).getCreated()).isNotNull();
    assertThat(sortedResults1.get(1).getCreated()).isNotNull();
    assertThat(sortedResults1.get(2).getCreated()).isNotNull();
    assertThat(sortedResults1.get(0).getUpdated()).isNotNull();
    assertThat(sortedResults1.get(1).getUpdated()).isNotNull();
    assertThat(sortedResults1.get(2).getUpdated()).isNotNull();
    assertThat(sortedResults1.get(0).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults1.get(1).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults1.get(2).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults1.get(0).getTiles()).isNotNull();
    assertThat(sortedResults1.get(1).getTiles()).isNotNull();
    assertThat(sortedResults1.get(2).getTiles()).isNotNull();

    DesignDocuments results2 = listDesigns(authorization, false);

    List<DesignDocument> sortedResults2 = results2.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults2).hasSize(2);

    assertThat(sortedResults2.get(0).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2);
    assertThat(sortedResults2.get(1).getUuid()).isEqualTo(TestConstants.DESIGN_UUID_3);
    assertThat(sortedResults2.get(0).getChecksum()).isEqualTo(Checksum.of(JSON_2));
    assertThat(sortedResults2.get(1).getChecksum()).isEqualTo(Checksum.of(JSON_3));
    assertThat(sortedResults2.get(0).getRevision()).isNotNull();
    assertThat(sortedResults2.get(1).getRevision()).isNotNull();
    assertThat(sortedResults2.get(0).getCreated()).isNotNull();
    assertThat(sortedResults2.get(1).getCreated()).isNotNull();
    assertThat(sortedResults2.get(0).getUpdated()).isNotNull();
    assertThat(sortedResults2.get(1).getUpdated()).isNotNull();
    assertThat(sortedResults2.get(0).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults2.get(1).getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(sortedResults2.get(0).getTiles()).isNotNull();
    assertThat(sortedResults2.get(1).getTiles()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is guest")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

    DesignDocument result1 = loadDesign(authorization, TestConstants.DESIGN_UUID_1, true);

    String json1 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT1)).toString();
    assertThat(result1.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_1);
    assertThat(result1.getJson()).isEqualTo(json1);
    assertThat(result1.getCreated()).isNotNull();
    assertThat(result1.getUpdated()).isNotNull();
    assertThat(result1.getChecksum()).isNotNull();
    assertThat(result1.getRevision()).isNotNull();
    assertThat(result1.getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(result1.getTiles()).isNotNull();

    DesignDocument result2 = loadDesign(authorization, TestConstants.DESIGN_UUID_2, false);

    String json2 = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT2)).toString();
    assertThat(result2.getUuid()).isEqualTo(TestConstants.DESIGN_UUID_2);
    assertThat(result2.getJson()).isEqualTo(json2);
    assertThat(result2.getCreated()).isNotNull();
    assertThat(result2.getUpdated()).isNotNull();
    assertThat(result2.getChecksum()).isNotNull();
    assertThat(result2.getRevision()).isNotNull();
    assertThat(result2.getLevels()).isEqualTo(TestConstants.LEVELS);
    assertThat(result2.getTiles()).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is guest")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

    byte[] result1 = getTile(authorization, TestConstants.DESIGN_UUID_2, true);

    final BufferedImage image1 = ImageIO.read(new ByteArrayInputStream(result1));
    assertThat(image1.getWidth()).isEqualTo(256);
    assertThat(image1.getHeight()).isEqualTo(256);

    byte[] result2 = getTile(authorization, TestConstants.DESIGN_UUID_2, false);

    final BufferedImage image2 = ImageIO.read(new ByteArrayInputStream(result2));
    assertThat(image2.getWidth()).isEqualTo(256);
    assertThat(image2.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/id does not exist")
  public void shouldReturnNotFoundWhenDesignDoesNotExist() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_0 + "?draft=true"))
            .then().assertThat().statusCode(404);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_1 + "?draft=false"))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/designId/level/col/row/256.png does not exist")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeDoesNotExist() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_0 + "/0/0/0/256.png?draft=true"))
            .then().assertThat().statusCode(404);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + TestConstants.DESIGN_UUID_1 + "/0/0/0/256.png?draft=false"))
            .then().assertThat().statusCode(404);
  }

  private static DesignDocuments listDesigns(String authorization) throws MalformedURLException {
    return listDesigns(authorization, true);
  }

  private static DesignDocument loadDesign(String authorization, UUID uuid) throws MalformedURLException {
    return loadDesign(authorization, uuid, true);
  }

  private static byte[] getTile(String authorization, UUID uuid) throws MalformedURLException {
    return getTile(authorization, uuid, true);
  }

  private static DesignDocuments listDesigns(String authorization, boolean draft) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs" + (draft ? "?draft=true" : "")))
            .then().assertThat().statusCode(200)
            .extract().body().as(DesignDocuments.class);
  }

  private static DesignDocument loadDesign(String authorization, UUID uuid, boolean draft) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + uuid + (draft ? "?draft=true" : "")))
            .then().assertThat().statusCode(200)
            .extract().body().as(DesignDocument.class);
  }

  private static byte[] getTile(String authorization, UUID uuid, boolean draft) throws MalformedURLException {
    return given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png" + (draft ? "?draft=true" : "")))
            .then().assertThat().statusCode(200)
            .and().assertThat().contentType("image/png")
            .extract().response().asByteArray();
  }
}
