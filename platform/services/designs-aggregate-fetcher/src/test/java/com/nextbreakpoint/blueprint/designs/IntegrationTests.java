package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;
import org.junit.jupiter.api.*;
import rx.Single;

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

public class IntegrationTests {
  private static final String SCRIPT1 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String SCRIPT2 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 100] (mod2(x) > 30) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String SCRIPT3 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 100] (mod2(x) > 20) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";
  private static final UUID DESIGN_UUID_0 = new UUID(0L, 0L);
  private static final UUID DESIGN_UUID_1 = new UUID(1L, 1L);
  private static final UUID DESIGN_UUID_2 = new UUID(1L, 2L);
  private static final UUID DESIGN_UUID_3 = new UUID(1L, 3L);
  private static final UUID DESIGN_UUID_4 = new UUID(1L, 4L);

  private static final TestScenario scenario = new TestScenario();

  private static Environment environment = Environment.getDefaultEnvironment();

  private static CassandraClient session;

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();

    final Vertx vertx = new Vertx(io.vertx.core.Vertx.vertx());

    session = CassandraClientFactory.create(environment, vertx, scenario.createCassandraConfig());
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    if (session != null) {
      try {
        session.close();
      } catch (Exception ignore) {
      }
    }

    scenario.after();
  }

  @Nested
  @Tag("slow")
  @Tag("integration")
  @DisplayName("Verify behaviour of designs-aggregate-fetcher service")
  public class VerifyServiceApi {
    @BeforeEach
    public void setup() {
      session.rxPrepare("TRUNCATE DESIGN_ENTITY")
              .map(PreparedStatement::bind)
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      final String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
      final String json2 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT2)).toString();
      final String json3 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT3)).toString();
      final String json4 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT3)).toString();

      final Single<PreparedStatement> preparedStatementSingle = session.rxPrepare("INSERT INTO DESIGN_ENTITY (DESIGN_UUID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_CREATED, DESIGN_UPDATED) VALUES (?,?,?,toTimeStamp(now()),toTimeStamp(now()))");

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_1, json1, "1"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_2, json2, "2"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_3, json3, "3"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_4, json4, "4"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      session.rxPrepare("DELETE FROM DESIGN_ENTITY WHERE DESIGN_UUID=?")
              .map(stmt -> stmt.bind(DESIGN_UUID_4))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();
    }

    @AfterEach
    public void reset() {
      RestAssured.reset();
    }

    @Test
    @DisplayName("Should allow OPTIONS on /designs without access token")
    public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().options(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should allow OPTIONS on /v1/designs/id without access token")
    public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().options(scenario.makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Should forbid GET on /v1/designs when user has unknown authority")
    public void shouldForbidGetOnDesignsWhenUserHasUnknownAuthority() throws MalformedURLException {
      final String otherAuthorization = scenario.makeAuthorization("test", "other");

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid GET on /v1/designs/id when user has unknown authority")
    public void shouldForbidGetOnDesignsSlashIdWhenUserHasUnknownAuthority() throws MalformedURLException {
      final String otherAuthorization = scenario.makeAuthorization("test", "other");

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs/" + DESIGN_UUID_1))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should forbid GET on /v1/designs/id/zoom/x/y/256.png when user has unknown authority")
    public void shouldForbidGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserHasUnknownAuthority() throws MalformedURLException {
      final String otherAuthorization = scenario.makeAuthorization("test", "other");

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept("image/png")
              .when().get(scenario.makeBaseURL("/v1/designs/" + DESIGN_UUID_2 + "/0/0/0/256.png"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs when user is anonymous")
    public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      DesignDocument[] results = listDesigns(authorization);

      List<DesignDocument> sortedResults = Stream.of(results)
              .sorted(Comparator.comparing(DesignDocument::getUuid))
              .collect(Collectors.toList());

      assertThat(sortedResults.get(0).getUuid()).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(sortedResults.get(1).getUuid()).isEqualTo(DESIGN_UUID_2.toString());
      assertThat(sortedResults.get(2).getUuid()).isEqualTo(DESIGN_UUID_3.toString());
      assertThat(sortedResults.get(0).getChecksum()).isEqualTo("1");
      assertThat(sortedResults.get(1).getChecksum()).isEqualTo("2");
      assertThat(sortedResults.get(2).getChecksum()).isEqualTo("3");
      assertThat(sortedResults.get(0).getModified()).isNotNull();
      assertThat(sortedResults.get(1).getModified()).isNotNull();
      assertThat(sortedResults.get(2).getModified()).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id when user is anonymous")
    public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      JsonPath result = loadDesign(authorization, DESIGN_UUID_1);

      String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
      assertThat(result.getString("uuid")).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(result.getString("json")).isEqualTo(json1);
      assertThat(result.getString("modified")).isNotNull();
      assertThat(result.getString("checksum")).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id/zoom/x/y/256.png when user is anonymous")
    public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      byte[] result = getTile(authorization, DESIGN_UUID_2);

      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
      assertThat(image.getWidth()).isEqualTo(256);
      assertThat(image.getHeight()).isEqualTo(256);
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs when user is admin")
    public void shouldAllowGetOnDesignsWhenUserIsAdmin() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);;

      pause();

      DesignDocument[] results = listDesigns(authorization);

      List<DesignDocument> sortedResults = Stream.of(results)
              .sorted(Comparator.comparing(DesignDocument::getUuid))
              .collect(Collectors.toList());

      assertThat(sortedResults.get(0).getUuid()).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(sortedResults.get(1).getUuid()).isEqualTo(DESIGN_UUID_2.toString());
      assertThat(sortedResults.get(2).getUuid()).isEqualTo(DESIGN_UUID_3.toString());
      assertThat(sortedResults.get(0).getChecksum()).isEqualTo("1");
      assertThat(sortedResults.get(1).getChecksum()).isEqualTo("2");
      assertThat(sortedResults.get(2).getChecksum()).isEqualTo("3");
      assertThat(sortedResults.get(0).getModified()).isNotNull();
      assertThat(sortedResults.get(1).getModified()).isNotNull();
      assertThat(sortedResults.get(2).getModified()).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id when user is admin")
    public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);;

      pause();

      JsonPath result = loadDesign(authorization, DESIGN_UUID_1);

      String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
      assertThat(result.getString("uuid")).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(result.getString("json")).isEqualTo(json1);
      assertThat(result.getString("modified")).isNotNull();
      assertThat(result.getString("checksum")).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id/zoom/x/y/256.png when user is admin")
    public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);;

      pause();

      byte[] result = getTile(authorization, DESIGN_UUID_2);

      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
      assertThat(image.getWidth()).isEqualTo(256);
      assertThat(image.getHeight()).isEqualTo(256);
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs when user is guest")
    public void shouldAllowGetOnDesignsWhenUserIsGuest() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.GUEST);;

      pause();

      DesignDocument[] results = listDesigns(authorization);

      List<DesignDocument> sortedResults = Stream.of(results)
              .sorted(Comparator.comparing(DesignDocument::getUuid))
              .collect(Collectors.toList());

      assertThat(sortedResults.get(0).getUuid()).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(sortedResults.get(1).getUuid()).isEqualTo(DESIGN_UUID_2.toString());
      assertThat(sortedResults.get(2).getUuid()).isEqualTo(DESIGN_UUID_3.toString());
      assertThat(sortedResults.get(0).getChecksum()).isEqualTo("1");
      assertThat(sortedResults.get(1).getChecksum()).isEqualTo("2");
      assertThat(sortedResults.get(2).getChecksum()).isEqualTo("3");
      assertThat(sortedResults.get(0).getModified()).isNotNull();
      assertThat(sortedResults.get(1).getModified()).isNotNull();
      assertThat(sortedResults.get(2).getModified()).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id when user is guest")
    public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.GUEST);;

      pause();

      JsonPath result = loadDesign(authorization, DESIGN_UUID_1);

      String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
      assertThat(result.getString("uuid")).isEqualTo(DESIGN_UUID_1.toString());
      assertThat(result.getString("json")).isEqualTo(json1);
      assertThat(result.getString("modified")).isNotNull();
      assertThat(result.getString("checksum")).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id/zoom/x/y/256.png when user is guest")
    public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
      final String authorization = scenario.makeAuthorization("test", Authority.GUEST);;

      pause();

      byte[] result = getTile(authorization, DESIGN_UUID_2);

      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
      assertThat(image.getWidth()).isEqualTo(256);
      assertThat(image.getHeight()).isEqualTo(256);
    }

    @Test
    @DisplayName("Should return NOT_FOUND when /v1/designs/id does not exist")
    public void shouldReturnNotFoundWhenDeisgnDoesNotExist() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs/" + DESIGN_UUID_0))
              .then().assertThat().statusCode(404);
    }

    @Test
    @DisplayName("Should return NOT_FOUND when /v1/designs/id/zoom/x/y/256.png does not exist")
    public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeDoesNotExist() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept("image/png")
              .when().get(scenario.makeBaseURL("/v1/designs/" + DESIGN_UUID_0 + "/0/0/0/256.png"))
              .then().assertThat().statusCode(404);
    }

    @Test
    @DisplayName("Should return NOT_FOUND when /v1/designs/id has been deleted")
    public void shouldReturnNotFoundWhenDeisgnHasBeenDeleted() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs/" + DESIGN_UUID_4))
              .then().assertThat().statusCode(404);
    }

    @Test
    @DisplayName("Should return NOT_FOUND when /v1/designs/id/zoom/x/y/256.png  has been deleted")
    public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeHasBeenDeleted() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept("image/png")
              .when().get(scenario.makeBaseURL("/v1/designs/" + DESIGN_UUID_4 + "/0/0/0/256.png"))
              .then().assertThat().statusCode(404);
    }
  }

  private static void pause() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException ignored) {
    }
  }

  private static DesignDocument[] listDesigns(String authorization) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(scenario.makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(200)
            .extract().body().as(DesignDocument[].class);
  }

  private static JsonPath loadDesign(String authorization, UUID uuid) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(scenario.makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .extract().body().jsonPath();
  }

  private static byte[] getTile(String authorization, UUID uuid) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(scenario.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().assertThat().contentType("image/png")
            .extract().response().asByteArray();
  }

  private static Map<String, Object> createPostData(String manifest, String metadata, String script) {
    final Map<String, Object> data = new HashMap<>();
    data.put("manifest", manifest);
    data.put("metadata", metadata);
    data.put("script", script);
    return data;
  }
}
