package com.nextbreakpoint.blueprint.designs;

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
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.SoftAssertions;
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
import java.net.MalformedURLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.COMMAND_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_4;
import static com.nextbreakpoint.blueprint.designs.TestConstants.DESIGN_ID_5;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_DRAFT;
import static com.nextbreakpoint.blueprint.designs.TestConstants.LEVELS_READY;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MANIFEST;
import static com.nextbreakpoint.blueprint.designs.TestConstants.MESSAGE_SOURCE;
import static com.nextbreakpoint.blueprint.designs.TestConstants.METADATA;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_0;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.REVISION_2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT2;
import static com.nextbreakpoint.blueprint.designs.TestConstants.SCRIPT3;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_1;
import static com.nextbreakpoint.blueprint.designs.TestConstants.USER_ID_2;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("integration")
@DisplayName("Verify behaviour of designs-query service")
public class IntegrationTests {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

  private static final TestCases testCases = new TestCases("DesignsQueryIntegrationTests");

  private static final String DATA_1 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
  private static final String DATA_2 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT2)).toString();
  private static final String DATA_3 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT3)).toString();

  private final LocalDateTime createTime = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneId.of("UTC")).minusDays(1);
  private final LocalDateTime updateTime = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneId.of("UTC"));

  private final DesignDocumentUpdateRequestedOutputMapper designDocumentUpdateRequestedMapper = new DesignDocumentUpdateRequestedOutputMapper(MESSAGE_SOURCE);
  private final DesignDocumentDeleteRequestedOutputMapper designDocumentDeleteRequestedMapper = new DesignDocumentDeleteRequestedOutputMapper(MESSAGE_SOURCE);

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
    testCases.deleteData();
    testCases.getSteps().reset();

    final Design design1 = new Design(DESIGN_ID_1, USER_ID_1, UUID.randomUUID(), DATA_1, Checksum.of(DATA_1), REVISION_0, "CREATED", false, LEVELS_DRAFT, TestUtils.getTiles(LEVELS_DRAFT, 0.5f), FORMATTER.format(Instant.now()), FORMATTER.format(Instant.now()));
    final Design design2 = new Design(DESIGN_ID_2, USER_ID_1, UUID.randomUUID(), DATA_2, Checksum.of(DATA_2), REVISION_0, "UPDATED", false, LEVELS_DRAFT, TestUtils.getTiles(LEVELS_DRAFT, 1.0f), FORMATTER.format(Instant.now()), FORMATTER.format(Instant.now()));
    final Design design3 = new Design(DESIGN_ID_3, USER_ID_1, UUID.randomUUID(), DATA_3, Checksum.of(DATA_3), REVISION_0, "UPDATED", true, LEVELS_READY, TestUtils.getTiles(LEVELS_READY, 1.0f), FORMATTER.format(Instant.now()), FORMATTER.format(Instant.now()));

    List.of(design1, design2, design3).forEach(testCases::insertDraftDesign);
    List.of(design3).forEach(testCases::insertDesign);
  }

  @AfterEach
  public void reset() {
    RestAssured.reset();
  }

  @Test
  @DisplayName("Should update the design after receiving a DesignDocumentUpdateRequested event")
  public void shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequested() {
    final List<Tiles> tiles1 = TestUtils.getTiles(LEVELS_DRAFT, 0f);
    final List<Tiles> tiles2 = TestUtils.getTiles(LEVELS_DRAFT, 50f);
    final List<Tiles> tiles3 = TestUtils.getTiles(LEVELS_READY, 100f);

    var designDocumentUpdateRequested1 = DesignDocumentUpdateRequested.builder()
            .withDesignId(DESIGN_ID_4)
            .withCommandId(COMMAND_ID_1)
            .withUserId(USER_ID_1)
            .withData(DATA_1)
            .withChecksum(Checksum.of(DATA_1))
            .withRevision(REVISION_0)
            .withLevels(LEVELS_DRAFT)
            .withTiles(tiles1)
            .withStatus("CREATED")
            .withPublished(false)
            .withCreated(createTime)
            .withUpdated(updateTime)
            .build();

    var designDocumentUpdateRequested2 = DesignDocumentUpdateRequested.builder()
            .withDesignId(DESIGN_ID_4)
            .withCommandId(COMMAND_ID_2)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_1)
            .withLevels(LEVELS_DRAFT)
            .withTiles(tiles2)
            .withStatus("UPDATED")
            .withPublished(false)
            .withCreated(createTime)
            .withUpdated(updateTime)
            .build();

    var designDocumentUpdateRequested3 = DesignDocumentUpdateRequested.builder()
            .withDesignId(DESIGN_ID_4)
            .withCommandId(COMMAND_ID_3)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withLevels(LEVELS_READY)
            .withTiles(tiles3)
            .withStatus("UPDATED")
            .withPublished(false)
            .withCreated(createTime)
            .withUpdated(updateTime)
            .build();

    var designDocumentUpdateRequested4 = DesignDocumentUpdateRequested.builder()
            .withDesignId(DESIGN_ID_4)
            .withCommandId(COMMAND_ID_4)
            .withUserId(USER_ID_1)
            .withData(DATA_2)
            .withChecksum(Checksum.of(DATA_2))
            .withRevision(REVISION_2)
            .withLevels(LEVELS_READY)
            .withTiles(tiles3)
            .withStatus("UPDATED")
            .withPublished(true)
            .withCreated(createTime)
            .withUpdated(updateTime)
            .build();

    final OutputMessage designDocumentUpdateRequestedMessage1 = designDocumentUpdateRequestedMapper.transform(designDocumentUpdateRequested1);
    final OutputMessage designDocumentUpdateRequestedMessage2 = designDocumentUpdateRequestedMapper.transform(designDocumentUpdateRequested2);
    final OutputMessage designDocumentUpdateRequestedMessage3 = designDocumentUpdateRequestedMapper.transform(designDocumentUpdateRequested3);
    final OutputMessage designDocumentUpdateRequestedMessage4 = designDocumentUpdateRequestedMapper.transform(designDocumentUpdateRequested4);

    final List<OutputMessage> designDocumentUpdateRequestedMessages = List.of(
            designDocumentUpdateRequestedMessage1,
            designDocumentUpdateRequestedMessage2,
            designDocumentUpdateRequestedMessage3,
            designDocumentUpdateRequestedMessage4
    );

    testCases.shouldUpdateTheDesignWhenReceivingADesignDocumentUpdateRequested(designDocumentUpdateRequestedMessages);
  }

  @Test
  @DisplayName("Should delete the design after receiving a DesignDocumentDeleteRequested event")
  public void shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequested() {
    final List<Tiles> tiles = TestUtils.getTiles(LEVELS_READY, 100.0f);

    var designDocumentUpdateRequested = DesignDocumentUpdateRequested.builder()
            .withDesignId(DESIGN_ID_5)
            .withCommandId(COMMAND_ID_1)
            .withUserId(USER_ID_2)
            .withData(DATA_1)
            .withChecksum(Checksum.of(DATA_1))
            .withRevision(REVISION_0)
            .withLevels(LEVELS_READY)
            .withTiles(tiles)
            .withStatus("UPDATED")
            .withPublished(true)
            .withCreated(createTime)
            .withUpdated(updateTime)
            .build();

    var designDocumentDeleteRequested = DesignDocumentDeleteRequested.builder()
            .withDesignId(DESIGN_ID_5)
            .withCommandId(COMMAND_ID_2)
            .withRevision(REVISION_0)
            .build();

    final OutputMessage designDocumentUpdateRequestedMessage = designDocumentUpdateRequestedMapper.transform(designDocumentUpdateRequested);
    final OutputMessage designDocumentDeleteRequestedMessage = designDocumentDeleteRequestedMapper.transform(designDocumentDeleteRequested);

    testCases.shouldDeleteTheDesignWhenReceivingADesignDocumentDeleteRequested(designDocumentUpdateRequestedMessage, designDocumentDeleteRequestedMessage);
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
            .when().get(testCases.makeBaseURL("/v1/designs/" + DESIGN_ID_1 + "?draft=true"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /v1/designs/designId/level/col/row/256.png when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = testCases.makeAuthorization("test", "other");

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + DESIGN_ID_2 + "/0/0/0/256.png?draft=true"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is anonymous")
  public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    DesignDocuments results = listDraftDesigns(authorization);

    List<DesignDocument> sortedResults = results.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults).hasSize(3);

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(sortedResults.get(0).getUuid()).isEqualTo(DESIGN_ID_1);
    softly.assertThat(sortedResults.get(1).getUuid()).isEqualTo(DESIGN_ID_2);
    softly.assertThat(sortedResults.get(2).getUuid()).isEqualTo(DESIGN_ID_3);
    softly.assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(DATA_1));
    softly.assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(DATA_2));
    softly.assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(DATA_3));
    softly.assertThat(sortedResults.get(0).getRevision()).isNotNull();
    softly.assertThat(sortedResults.get(1).getRevision()).isNotNull();
    softly.assertThat(sortedResults.get(2).getRevision()).isNotNull();
    softly.assertThat(sortedResults.get(0).getCreated()).isNotNull();
    softly.assertThat(sortedResults.get(1).getCreated()).isNotNull();
    softly.assertThat(sortedResults.get(2).getCreated()).isNotNull();
    softly.assertThat(sortedResults.get(0).getUpdated()).isNotNull();
    softly.assertThat(sortedResults.get(1).getUpdated()).isNotNull();
    softly.assertThat(sortedResults.get(2).getUpdated()).isNotNull();
    softly.assertThat(sortedResults.get(0).getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(sortedResults.get(1).getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(sortedResults.get(2).getLevels()).isEqualTo(LEVELS_READY);
    softly.assertThat(sortedResults.get(0).getTiles()).isNotNull();
    softly.assertThat(sortedResults.get(1).getTiles()).isNotNull();
    softly.assertThat(sortedResults.get(2).getTiles()).isNotNull();
    softly.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    DesignDocument result = loadDraftDesign(authorization, DESIGN_ID_1);

    String json1 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(result.getUuid()).isEqualTo(DESIGN_ID_1);
    softly.assertThat(result.getJson()).isEqualTo(json1);
    softly.assertThat(result.getCreated()).isNotNull();
    softly.assertThat(result.getUpdated()).isNotNull();
    softly.assertThat(result.getChecksum()).isNotNull();
    softly.assertThat(result.getRevision()).isNotNull();
    softly.assertThat(result.getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(result.getTiles()).isNotNull();
    softly.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    byte[] result = getDraftTile(authorization, DESIGN_ID_1);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(image.getWidth()).isEqualTo(256);
    softly.assertThat(image.getHeight()).isEqualTo(256);
    softly.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs when user is admin")
  public void shouldAllowGetOnDesignsWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    DesignDocuments results = listDraftDesigns(authorization);

    List<DesignDocument> sortedResults = results.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults).hasSize(3);

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(sortedResults.get(0).getUuid()).isEqualTo(DESIGN_ID_1);
    softly.assertThat(sortedResults.get(1).getUuid()).isEqualTo(DESIGN_ID_2);
    softly.assertThat(sortedResults.get(2).getUuid()).isEqualTo(DESIGN_ID_3);
    softly.assertThat(sortedResults.get(0).getChecksum()).isEqualTo(Checksum.of(DATA_1));
    softly.assertThat(sortedResults.get(1).getChecksum()).isEqualTo(Checksum.of(DATA_2));
    softly.assertThat(sortedResults.get(2).getChecksum()).isEqualTo(Checksum.of(DATA_3));
    softly.assertThat(sortedResults.get(0).getRevision()).isNotNull();
    softly.assertThat(sortedResults.get(1).getRevision()).isNotNull();
    softly.assertThat(sortedResults.get(2).getRevision()).isNotNull();
    softly.assertThat(sortedResults.get(0).getCreated()).isNotNull();
    softly.assertThat(sortedResults.get(1).getCreated()).isNotNull();
    softly.assertThat(sortedResults.get(2).getCreated()).isNotNull();
    softly.assertThat(sortedResults.get(0).getUpdated()).isNotNull();
    softly.assertThat(sortedResults.get(1).getUpdated()).isNotNull();
    softly.assertThat(sortedResults.get(2).getUpdated()).isNotNull();
    softly.assertThat(sortedResults.get(0).getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(sortedResults.get(1).getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(sortedResults.get(2).getLevels()).isEqualTo(LEVELS_READY);
    softly.assertThat(sortedResults.get(0).getTiles()).isNotNull();
    softly.assertThat(sortedResults.get(1).getTiles()).isNotNull();
    softly.assertThat(sortedResults.get(2).getTiles()).isNotNull();
    softly.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is admin")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    DesignDocument result = loadDraftDesign(authorization, DESIGN_ID_1);

    String json1 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(result.getUuid()).isEqualTo(DESIGN_ID_1);
    softly.assertThat(result.getJson()).isEqualTo(json1);
    softly.assertThat(result.getCreated()).isNotNull();
    softly.assertThat(result.getUpdated()).isNotNull();
    softly.assertThat(result.getChecksum()).isNotNull();
    softly.assertThat(result.getRevision()).isNotNull();
    softly.assertThat(result.getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(result.getTiles()).isNotNull();
    softly.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is admin")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.ADMIN);

    byte[] result = getDraftTile(authorization, DESIGN_ID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(image.getWidth()).isEqualTo(256);
    softly.assertThat(image.getHeight()).isEqualTo(256);
    softly.assertAll();
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

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(sortedResults1.get(0).getUuid()).isEqualTo(DESIGN_ID_1);
    softly.assertThat(sortedResults1.get(1).getUuid()).isEqualTo(DESIGN_ID_2);
    softly.assertThat(sortedResults1.get(2).getUuid()).isEqualTo(DESIGN_ID_3);
    softly.assertThat(sortedResults1.get(0).getChecksum()).isEqualTo(Checksum.of(DATA_1));
    softly.assertThat(sortedResults1.get(1).getChecksum()).isEqualTo(Checksum.of(DATA_2));
    softly.assertThat(sortedResults1.get(2).getChecksum()).isEqualTo(Checksum.of(DATA_3));
    softly.assertThat(sortedResults1.get(0).getRevision()).isNotNull();
    softly.assertThat(sortedResults1.get(1).getRevision()).isNotNull();
    softly.assertThat(sortedResults1.get(2).getRevision()).isNotNull();
    softly.assertThat(sortedResults1.get(0).getCreated()).isNotNull();
    softly.assertThat(sortedResults1.get(1).getCreated()).isNotNull();
    softly.assertThat(sortedResults1.get(2).getCreated()).isNotNull();
    softly.assertThat(sortedResults1.get(0).getUpdated()).isNotNull();
    softly.assertThat(sortedResults1.get(1).getUpdated()).isNotNull();
    softly.assertThat(sortedResults1.get(2).getUpdated()).isNotNull();
    softly.assertThat(sortedResults1.get(0).getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(sortedResults1.get(1).getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(sortedResults1.get(2).getLevels()).isEqualTo(LEVELS_READY);
    softly.assertThat(sortedResults1.get(0).getTiles()).isNotNull();
    softly.assertThat(sortedResults1.get(1).getTiles()).isNotNull();
    softly.assertThat(sortedResults1.get(2).getTiles()).isNotNull();
    softly.assertAll();

    DesignDocuments results2 = listDesigns(authorization, false);

    List<DesignDocument> sortedResults2 = results2.getDesigns().stream()
            .sorted(Comparator.comparing(DesignDocument::getUuid))
            .collect(Collectors.toList());

    assertThat(sortedResults2).hasSize(1);

    SoftAssertions softly2 = new SoftAssertions();
    softly2.assertThat(sortedResults2.get(0).getUuid()).isEqualTo(DESIGN_ID_3);
    softly2.assertThat(sortedResults2.get(0).getChecksum()).isEqualTo(Checksum.of(DATA_3));
    softly2.assertThat(sortedResults2.get(0).getRevision()).isNotNull();
    softly2.assertThat(sortedResults2.get(0).getCreated()).isNotNull();
    softly2.assertThat(sortedResults2.get(0).getUpdated()).isNotNull();
    softly2.assertThat(sortedResults2.get(0).getLevels()).isEqualTo(LEVELS_READY);
    softly2.assertThat(sortedResults2.get(0).getTiles()).isNotNull();
    softly2.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/id when user is guest")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

    DesignDocument result1 = loadDesign(authorization, DESIGN_ID_1, true);

    String json1 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT1)).toString();

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(result1.getUuid()).isEqualTo(DESIGN_ID_1);
    softly.assertThat(result1.getJson()).isEqualTo(json1);
    softly.assertThat(result1.getCreated()).isNotNull();
    softly.assertThat(result1.getUpdated()).isNotNull();
    softly.assertThat(result1.getChecksum()).isNotNull();
    softly.assertThat(result1.getRevision()).isNotNull();
    softly.assertThat(result1.getLevels()).isEqualTo(LEVELS_DRAFT);
    softly.assertThat(result1.getTiles()).isNotNull();
    softly.assertAll();

    DesignDocument result2 = loadDesign(authorization, DESIGN_ID_3, false);

    String json2 = new JsonObject(TestUtils.createPostData(MANIFEST, METADATA, SCRIPT3)).toString();

    SoftAssertions softly2 = new SoftAssertions();
    softly2.assertThat(result2.getUuid()).isEqualTo(DESIGN_ID_3);
    softly2.assertThat(result2.getJson()).isEqualTo(json2);
    softly2.assertThat(result2.getCreated()).isNotNull();
    softly2.assertThat(result2.getUpdated()).isNotNull();
    softly2.assertThat(result2.getChecksum()).isNotNull();
    softly2.assertThat(result2.getRevision()).isNotNull();
    softly2.assertThat(result2.getLevels()).isEqualTo(LEVELS_READY);
    softly2.assertThat(result2.getTiles()).isNotNull();
    softly2.assertAll();
  }

  @Test
  @DisplayName("Should allow GET on /v1/designs/designId/level/col/row/256.png when user is guest")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
    final String authorization = testCases.makeAuthorization("test", Authority.GUEST);

    byte[] result1 = getTile(authorization, DESIGN_ID_3, true);

    final BufferedImage image1 = ImageIO.read(new ByteArrayInputStream(result1));

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(image1.getWidth()).isEqualTo(256);
    softly.assertThat(image1.getHeight()).isEqualTo(256);
    softly.assertAll();

    byte[] result2 = getTile(authorization, DESIGN_ID_3, false);

    final BufferedImage image2 = ImageIO.read(new ByteArrayInputStream(result2));

    SoftAssertions softly2 = new SoftAssertions();
    softly2.assertThat(image2.getWidth()).isEqualTo(256);
    softly2.assertThat(image2.getHeight()).isEqualTo(256);
    softly2.assertAll();
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/id does not exist")
  public void shouldReturnNotFoundWhenDesignDoesNotExist() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + DESIGN_ID_0 + "?draft=true"))
            .then().assertThat().statusCode(404);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(testCases.makeBaseURL("/v1/designs/" + DESIGN_ID_1 + "?draft=false"))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /v1/designs/designId/level/col/row/256.png does not exist")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeDoesNotExist() throws MalformedURLException {
    final String authorization = testCases.makeAuthorization("test", Authority.ANONYMOUS);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + DESIGN_ID_0 + "/0/0/0/256.png?draft=true"))
            .then().assertThat().statusCode(404);

    given().config(TestUtils.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(testCases.makeBaseURL("/v1/designs/" + DESIGN_ID_1 + "/0/0/0/256.png?draft=false"))
            .then().assertThat().statusCode(404);
  }

  private static DesignDocuments listDraftDesigns(String authorization) throws MalformedURLException {
    return listDesigns(authorization, true);
  }

  private static DesignDocument loadDraftDesign(String authorization, UUID uuid) throws MalformedURLException {
    return loadDesign(authorization, uuid, true);
  }

  private static byte[] getDraftTile(String authorization, UUID uuid) throws MalformedURLException {
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
