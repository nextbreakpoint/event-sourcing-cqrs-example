package com.nextbreakpoint.shop.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.shop.common.model.Authority;
import com.nextbreakpoint.shop.common.model.DesignDocument;
import com.nextbreakpoint.shop.common.vertx.TestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@DisplayName("Verify contract of service Designs Query")
public class VerticleIT {
  private static final String JSON_1 = "{\"metadata\":\"{\\\"translation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":1.0,\\\"w\\\":0.0},\\\"rotation\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0,\\\"z\\\":0.0,\\\"w\\\":0.0},\\\"scale\\\":{\\\"x\\\":1.0,\\\"y\\\":1.0,\\\"z\\\":1.0,\\\"w\\\":1.0},\\\"point\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"julia\\\":false,\\\"options\\\":{\\\"showPreview\\\":false,\\\"showTraps\\\":false,\\\"showOrbit\\\":false,\\\"showPoint\\\":false,\\\"previewOrigin\\\":{\\\"x\\\":0.0,\\\"y\\\":0.0},\\\"previewSize\\\":{\\\"x\\\":0.25,\\\"y\\\":0.25}}}\",\"manifest\":\"{\\\"pluginId\\\":\\\"Mandelbrot\\\"}\",\"script\":\"fractal {\\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\\nloop [0, 200] (mod2(x) > 40) {\\nx = x * x + w;\\n}\\n}\\ncolor [#FF000000] {\\npalette gradient {\\n[#FFFFFFFF > #FF000000, 100];\\n[#FF000000 > #FFFFFFFF, 100];\\n}\\ninit {\\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\\n}\\nrule (n > 0) [1] {\\ngradient[m - 1]\\n}\\n}\\n}\\n\"}";

  private static final String UUID_1 = "48f6563e-e095-4d91-b473-4d8afc758c43";
  private static final String UUID_2 = "afb0f952-b26a-46de-8e65-40d5e98dc243";
  private static final String UUID_3 = "51a0ce28-10a7-4a2e-ac6b-b80f682c0bb7";

  private static final UUID NON_EXISTANT_UUID = new UUID(0, 0);

  private static RestAssuredConfig restAssuredConfig;

  @BeforeAll
  public static void configureRestAssured() {
    final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
    final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
    final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
    restAssuredConfig = RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
  }

  @AfterAll
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  @Test
  @DisplayName("Should allow OPTIONS on /q/designs without access token")
  public void shouldAllowOptionsOnDesignsWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/q/designs"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should allow OPTIONS on /q/designs/id without access token")
  public void shouldAllowOptionsOnDesignsSlashIdWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/q/designs/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("Should forbid GET on /q/designs when user has unknown authority")
  public void shouldForbidGetOnDesignsWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = TestHelper.makeAuthorization("test", Arrays.asList("other"));

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/q/designs"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /q/designs/id when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = TestHelper.makeAuthorization("test", Arrays.asList("other"));

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/q/designs/" + UUID_1))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should forbid GET on /q/designs/id/zoom/x/y/256.png when user has unknown authority")
  public void shouldForbidGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserHasUnknownAuthority() throws MalformedURLException {
    final String otherAuthorization = TestHelper.makeAuthorization("test", Arrays.asList("other"));

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/q/designs/" + UUID_2 + "/0/0/0/256.png"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("Should allow GET on /q/designs when user is anonymous")
  public void shouldAllowGetOnDesignsWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS));

    pause();

    DesignDocument[] results = listDesigns(authorization);

    DesignDocument document1 = new DesignDocument(UUID_1, null, "0", null);
    DesignDocument document2 = new DesignDocument(UUID_2, null, "1", null);
    DesignDocument document3 = new DesignDocument(UUID_3, null, "2", null);

    assertThat(results).contains(document1, document2, document3);
  }

  @Test
  @DisplayName("Should allow GET on /q/designs/id when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS));

    pause();

    JsonPath result = loadDesign(authorization, UUID_1);

    assertThat(result.getString("uuid")).isEqualTo(UUID_1);
    assertThat(result.getString("json")).isEqualTo(JSON_1);
    assertThat(result.getString("modified")).isNotNull();
    assertThat(result.getString("checksum")).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /q/designs/id/zoom/x/y/256.png when user is anonymous")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAnonymous() throws IOException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS));

    pause();

    byte[] result = getTile(authorization, UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /q/designs when user is admin")
  public void shouldAllowGetOnDesignsWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    DesignDocument[] results = listDesigns(authorization);

    DesignDocument document1 = new DesignDocument(UUID_1, null, "0", null);
    DesignDocument document2 = new DesignDocument(UUID_2, null, "1", null);
    DesignDocument document3 = new DesignDocument(UUID_3, null, "2", null);

    assertThat(results).contains(document1, document2, document3);
  }

  @Test
  @DisplayName("Should allow GET on /q/designs/id when user is admin")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsAdmin() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    JsonPath result = loadDesign(authorization, UUID_1);

    assertThat(result.getString("uuid")).isEqualTo(UUID_1);
    assertThat(result.getString("json")).isEqualTo(JSON_1);
    assertThat(result.getString("modified")).isNotNull();
    assertThat(result.getString("checksum")).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /q/designs/id/zoom/x/y/256.png when user is admin")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsAdmin() throws IOException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    byte[] result = getTile(authorization, UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should allow GET on /q/designs when user is guest")
  public void shouldAllowGetOnDesignsWhenUserIsGuest() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.GUEST));

    pause();

    DesignDocument[] results = listDesigns(authorization);

    DesignDocument document1 = new DesignDocument(UUID_1, null, "0", null);
    DesignDocument document2 = new DesignDocument(UUID_2, null, "1", null);
    DesignDocument document3 = new DesignDocument(UUID_3, null, "2", null);

    assertThat(results).contains(document1, document2, document3);
  }

  @Test
  @DisplayName("Should allow GET on /q/designs/id when user is guest")
  public void shouldAllowGetOnDesignsSlashIdWhenUserIsGuest() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.GUEST));

    pause();

    JsonPath result = loadDesign(authorization, UUID_1);

    assertThat(result.getString("uuid")).isEqualTo(UUID_1);
    assertThat(result.getString("json")).isEqualTo(JSON_1);
    assertThat(result.getString("modified")).isNotNull();
    assertThat(result.getString("checksum")).isNotNull();
  }

  @Test
  @DisplayName("Should allow GET on /q/designs/id/zoom/x/y/256.png when user is guest")
  public void shouldAllowGetOnDesignsSlashIdSlashLocationSlashSizeWhenUserIsGuest() throws IOException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.GUEST));

    pause();

    byte[] result = getTile(authorization, UUID_2);

    final BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(256);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /q/designs/id does not exist")
  public void shouldReturnNotFoundWhenDeisgnDoesNotExist() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS));

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/q/designs/" + NON_EXISTANT_UUID.toString()))
            .then().assertThat().statusCode(404);
  }

  @Test
  @DisplayName("Should return NOT_FOUND when /q/designs/id/zoom/x/y/256.png does not exist")
  public void shouldReturnNotFoundWhenDesignSlashLocationSlashSizeDoesNotExist() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ANONYMOUS));

    pause();

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/q/designs/" + NON_EXISTANT_UUID.toString() + "/0/0/0/256.png"))
            .then().assertThat().statusCode(404);
  }

  private void pause() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }
  }

  protected DesignDocument[] listDesigns(String authorization) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/q/designs"))
            .then().assertThat().statusCode(200)
            .extract().body().as(DesignDocument[].class);
  }

  protected JsonPath loadDesign(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/q/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .extract().body().jsonPath();
  }

  protected byte[] getTile(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/q/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().assertThat().contentType("image/png")
            .extract().response().asByteArray();
  }

  private URL makeBaseURL(String path) throws MalformedURLException {
    final Integer port = Integer.getInteger("http.port", 3021);
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
      final String host = System.getProperty("http.host", "localhost");
      return new URL("https://" + host + ":" + port + "/" + normPath);
  }
}
