package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.DesignDocument;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
public class TestSuite {
  private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

  private static final String UUID_0 = new UUID(0, 0).toString();
  private static final String UUID_1 = "48f6563e-e095-4d91-b473-4d8afc758c43";
  private static final String UUID_2 = "2fe33eed-58f7-425b-b10c-2e490c0f248e";
  private static final String UUID_3 = "51a0ce28-10a7-4a2e-ac6b-b80f682c0bb7";
  private static final String UUID_4 = "afb0f952-b26a-46de-8e65-40d5e98dc243";

  private static final TestScenario scenario = new TestScenario();

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    scenario.after();
  }

  @Nested
  @Tag("integration")
  @DisplayName("Verify behaviour of designs-aggregate-fetcher service")
  public class VerifyServiceIntegration {
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
              .when().get(scenario.makeBaseURL("/v1/designs/" + UUID_1))
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
              .when().get(scenario.makeBaseURL("/v1/designs/" + UUID_2 + "/0/0/0/256.png"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs when user is anonymous")
    public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      DesignDocument[] results = listDesigns(authorization);

      assertThat(results[0].getUuid()).isEqualTo(UUID_1);
      assertThat(results[1].getUuid()).isEqualTo(UUID_2);
      assertThat(results[2].getUuid()).isEqualTo(UUID_3);
      assertThat(results[0].getChecksum()).isEqualTo("0");
      assertThat(results[1].getChecksum()).isEqualTo("4");
      assertThat(results[2].getChecksum()).isEqualTo("3");
      assertThat(results[0].getModified()).isNotNull();
      assertThat(results[1].getModified()).isNotNull();
      assertThat(results[2].getModified()).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id when user is anonymous")
    public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      JsonPath result = loadDesign(authorization, UUID_1);

      assertThat(result.getString("uuid")).isEqualTo(UUID_1);
      assertThat(result.getString("json")).isEqualTo(JSON_1);
      assertThat(result.getString("modified")).isNotNull();
      assertThat(result.getString("checksum")).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id/zoom/x/y/256.png when user is anonymous")
    public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
      final String authorization = scenario.makeAuthorization("test", Authority.ANONYMOUS);;

      pause();

      byte[] result = getTile(authorization, UUID_2);

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

      assertThat(results[0].getUuid()).isEqualTo(UUID_1);
      assertThat(results[1].getUuid()).isEqualTo(UUID_2);
      assertThat(results[2].getUuid()).isEqualTo(UUID_3);
      assertThat(results[0].getChecksum()).isEqualTo("0");
      assertThat(results[1].getChecksum()).isEqualTo("4");
      assertThat(results[2].getChecksum()).isEqualTo("3");
      assertThat(results[0].getModified()).isNotNull();
      assertThat(results[1].getModified()).isNotNull();
      assertThat(results[2].getModified()).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id when user is admin")
    public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);;

      pause();

      JsonPath result = loadDesign(authorization, UUID_1);

      assertThat(result.getString("uuid")).isEqualTo(UUID_1);
      assertThat(result.getString("json")).isEqualTo(JSON_1);
      assertThat(result.getString("modified")).isNotNull();
      assertThat(result.getString("checksum")).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id/zoom/x/y/256.png when user is admin")
    public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);;

      pause();

      byte[] result = getTile(authorization, UUID_2);

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

      assertThat(results[0].getUuid()).isEqualTo(UUID_1);
      assertThat(results[1].getUuid()).isEqualTo(UUID_2);
      assertThat(results[2].getUuid()).isEqualTo(UUID_3);
      assertThat(results[0].getChecksum()).isEqualTo("0");
      assertThat(results[1].getChecksum()).isEqualTo("4");
      assertThat(results[2].getChecksum()).isEqualTo("3");
      assertThat(results[0].getModified()).isNotNull();
      assertThat(results[1].getModified()).isNotNull();
      assertThat(results[2].getModified()).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id when user is guest")
    public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.GUEST);;

      pause();

      JsonPath result = loadDesign(authorization, UUID_1);

      assertThat(result.getString("uuid")).isEqualTo(UUID_1);
      assertThat(result.getString("json")).isEqualTo(JSON_1);
      assertThat(result.getString("modified")).isNotNull();
      assertThat(result.getString("checksum")).isNotNull();
    }

    @Test
    @DisplayName("Should allow GET on /v1/designs/id/zoom/x/y/256.png when user is guest")
    public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
      final String authorization = scenario.makeAuthorization("test", Authority.GUEST);;

      pause();

      byte[] result = getTile(authorization, UUID_2);

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
              .when().get(scenario.makeBaseURL("/v1/designs/" + UUID_0))
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
              .when().get(scenario.makeBaseURL("/v1/designs/" + UUID_0 + "/0/0/0/256.png"))
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
              .when().get(scenario.makeBaseURL("/v1/designs/" + UUID_4))
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
              .when().get(scenario.makeBaseURL("/v1/designs/" + UUID_4 + "/0/0/0/256.png"))
              .then().assertThat().statusCode(404);
    }

    private void pause() {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {
      }
    }

    protected DesignDocument[] listDesigns(String authorization) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(200)
              .extract().body().as(DesignDocument[].class);
    }

    protected JsonPath loadDesign(String authorization, String uuid) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs/" + uuid))
              .then().assertThat().statusCode(200)
              .extract().body().jsonPath();
    }

    protected byte[] getTile(String authorization, String uuid) throws MalformedURLException {
      return given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, authorization)
              .and().accept("image/png")
              .when().get(scenario.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
              .then().assertThat().statusCode(200)
              .and().assertThat().contentType("image/png")
              .extract().response().asByteArray();
    }
  }
}
