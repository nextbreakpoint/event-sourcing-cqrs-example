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
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.shop.common.model.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(JUnitPlatform.class)
@Tag("slow")
@DisplayName("Designs service")
public class VerticleIT {
  private static final String SCRIPT1 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String SCRIPT2 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 100] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";

  private static RestAssuredConfig restAssuredConfig;

  private Vertx vertx;

  private URL makeBaseURL(String path) throws MalformedURLException {
    final Integer port = Integer.getInteger("http.port", 3001);
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
    return new URL("https://localhost:" + port + "/" + normPath);
  }

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

  @BeforeEach
  public void createVertx() {
    vertx = new Vertx(io.vertx.core.Vertx.vertx());
  }

  @AfterEach
  public void destroyVertx() {
    vertx.close();
  }

  @Test
  @DisplayName("should allow options request without access token")
  public void shouldAllowOptionsRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/a/designs"))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");

    given().config(restAssuredConfig)
            .with().header("Origin", "https://localhost:8080")
            .when().options(makeBaseURL("/a/designs/" + UUID.randomUUID().toString()))
            .then().assertThat().statusCode(204)
            .and().header("Access-Control-Allow-Origin", "https://localhost:8080")
            .and().header("Access-Control-Allow-Credentials", "true");
  }

  @Test
  @DisplayName("should forbid post request without access token")
  public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
    given().config(restAssuredConfig)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(createPostData(SCRIPT1))
            .when().post(makeBaseURL("/a/designs"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid delete request without access token")
  public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String uuid = createDesign(authorization, createPostData(SCRIPT1));

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/a/designs/" + uuid))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should forbid get request when user doesn't have permissions")
  public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String uuid = createDesign(authorization, createPostData(SCRIPT1));

    final String otherAuthorization = TestHelper.makeAuthorization("test", Arrays.asList("other"));

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/designs"))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/designs/" + uuid))
            .then().assertThat().statusCode(403);

    given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, otherAuthorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/a/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(403);
  }

  @Test
  @DisplayName("should allow get request when user is anonymous")
  public void shouldAllowGetRequestWhenUserIsAnonymous() throws MalformedURLException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    final String uuid = createDesign(authorization, createPostData(SCRIPT1));

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/designs"))
            .then().assertThat().statusCode(200);

    given().config(restAssuredConfig)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/designs/" + uuid))
            .then().assertThat().statusCode(200);

    given().config(restAssuredConfig)
            .and().accept("image/png")
            .when().get(makeBaseURL("/a/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200);
  }

  @Test
  @DisplayName("should create and delete designs")
  public void shouldCreateAndDeleteDesigns() throws IOException {
    final String authorization = TestHelper.makeAuthorization("test", Arrays.asList(Authority.ADMIN));

    pause();

    KafkaConsumer<String, String> consumer = null;

    try {
//      consumer = KafkaClientFactory.createConsumer(vertx, createConsumerConfig("designs-test"));

      String[] message = new String[]{null};

//      consumer.handler(record -> message[0] = record.value())
//              .rxSubscribe("designs-sse")
//              .subscribe();

      long eventTimestamp0 = System.currentTimeMillis();

      final String uuid1 = createDesign(authorization, createPostData(SCRIPT1));

//      await().atMost(TWO_SECONDS)
//              .pollInterval(ONE_HUNDRED_MILLISECONDS)
//              .untilAsserted(() -> {
//                assertThat(message[0]).isNotNull();
//                Message actualMessage = Json.decodeValue(message[0], Message.class);
//                assertThat(actualMessage.getTimestamp()).isNotNull();
//                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
//                assertThat(actualMessage.getPartitionKey()).isEqualTo(uuid1);
//                assertThat(actualMessage.getMessageId()).isNotNull();
//                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
//                DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChangedEvent.class);
//                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(uuid1));
//                assertThat(actualEvent.getTimestamp()).isNotNull();
//                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp0);
//              });

        final JsonPath jsonPath0 = getDesign(authorization, uuid1);

        assertThat(jsonPath0.getString("uuid")).isEqualTo(uuid1);
        assertThat(jsonPath0.getString("json")).isNotNull();
        assertThat(jsonPath0.getString("modified")).isNotNull();
        assertThat(jsonPath0.getString("checksum")).isNotNull();

        final String json0 = jsonPath0.getString("json");
        System.out.println(json0);

        final Map<String, Object> object0 = Json.decodeValue(json0, HashMap.class);
        assertThat(object0.get("script")).isEqualTo(SCRIPT1);
        assertThat(object0.get("metadata")).isEqualTo(METADATA);
        assertThat(object0.get("manifest")).isEqualTo(MANIFEST);

        long eventTimestamp1 = System.currentTimeMillis();

      updateDesign(authorization, uuid1, createPostData(SCRIPT2));

//      await().atMost(TWO_SECONDS)
//              .pollInterval(ONE_HUNDRED_MILLISECONDS)
//              .untilAsserted(() -> {
//                assertThat(message[0]).isNotNull();
//                Message actualMessage = Json.decodeValue(message[0], Message.class);
//                assertThat(actualMessage.getTimestamp()).isNotNull();
//                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
//                assertThat(actualMessage.getPartitionKey()).isEqualTo(uuid1);
//                assertThat(actualMessage.getMessageId()).isNotNull();
//                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
//                DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChangedEvent.class);
//                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(uuid1));
//                assertThat(actualEvent.getTimestamp()).isNotNull();
//                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp1);
//              });

      final JsonPath jsonPath1 = getDesign(authorization, uuid1);

      assertThat(jsonPath1.getString("uuid")).isEqualTo(uuid1);
      assertThat(jsonPath1.getString("json")).isNotNull();
      assertThat(jsonPath1.getString("modified")).isNotNull();
      assertThat(jsonPath1.getString("checksum")).isNotNull();

      final String json1 = jsonPath1.getString("json");
      System.out.println(json1);

      final Map<String, Object> object1 = Json.decodeValue(json1, HashMap.class);
      assertThat(object1.get("script")).isEqualTo(SCRIPT2);
      assertThat(object1.get("metadata")).isEqualTo(METADATA);
      assertThat(object1.get("manifest")).isEqualTo(MANIFEST);

      final String uuid2 = createDesign(authorization, createPostData(SCRIPT1));

      final JsonPath jsonPath2 = getDesign(authorization, uuid2);

      assertThat(jsonPath2.getString("uuid")).isEqualTo(uuid2);
      assertThat(jsonPath2.getString("json")).isNotNull();
      assertThat(jsonPath2.getString("modified")).isNotNull();
      assertThat(jsonPath2.getString("checksum")).isNotNull();

      final String json2 = jsonPath2.getString("json");
      System.out.println(json2);

      final Map<String, Object> object2 = Json.decodeValue(json2, HashMap.class);
      assertThat(object2.get("script")).isEqualTo(SCRIPT1);
      assertThat(object2.get("metadata")).isEqualTo(METADATA);
      assertThat(object2.get("manifest")).isEqualTo(MANIFEST);

      final DesignDocument document1 = new DesignDocument(uuid1, null, jsonPath1.getString("checksum"), null);
      final DesignDocument document2 = new DesignDocument(uuid2, null, jsonPath2.getString("checksum"), null);

      assertThat(getDesigns(authorization)).contains(document1, document2);

      final byte[] bytes = getTile(authorization, uuid1);

      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      assertThat(image.getWidth()).isEqualTo(256);
      assertThat(image.getHeight()).isEqualTo(256);

      long eventTimestamp3 = System.currentTimeMillis();

      deleteDesign(authorization, uuid1);

//      await().atMost(TWO_SECONDS)
//              .pollInterval(ONE_HUNDRED_MILLISECONDS)
//              .untilAsserted(() -> {
//                assertThat(message[0]).isNotNull();
//                Message actualMessage = Json.decodeValue(message[0], Message.class);
//                assertThat(actualMessage.getTimestamp()).isNotNull();
//                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
//                assertThat(actualMessage.getPartitionKey()).isEqualTo(uuid1);
//                assertThat(actualMessage.getMessageId()).isNotNull();
//                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
//                DesignChangedEvent actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChangedEvent.class);
//                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(uuid1));
//                assertThat(actualEvent.getTimestamp()).isNotNull();
//                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp3);
//              });

      assertThat(getDesigns(authorization)).contains(document2);
      assertThat(getDesigns(authorization)).doesNotContain(document1);

      deleteDesign(authorization, uuid2);

      assertThat(getDesigns(authorization)).doesNotContain(document1, document2);
    } finally {
      if (consumer != null) {
        consumer.close();
      }
    }
  }

  private void pause() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }
  }

  private void deleteDesign(String authorization, String uuid) throws MalformedURLException {
    System.out.println("delete design " + uuid);
    given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(makeBaseURL("/a/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }

  private byte[] getTile(String authorization, String uuid) throws MalformedURLException {
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(makeBaseURL("/a/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().contentType("image/png")
            .and().extract().asByteArray();
  }

  private DesignDocument[] getDesigns(String authorization) throws MalformedURLException {
    System.out.println("get designs");
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(DesignDocument[].class);
  }

  private JsonPath getDesign(String authorization, String uuid) throws MalformedURLException {
    System.out.println("get design " + uuid);
    return given().config(restAssuredConfig)
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(makeBaseURL("/a/designs/" + uuid))
            .then().assertThat().statusCode(200).extract().jsonPath();
  }

  private String createDesign(String authorization, Map<String, String> design) throws MalformedURLException {
    System.out.println("create design");
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(design)
            .when().post(makeBaseURL("/a/designs"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private String updateDesign(String authorization, String uuid, Map<String, String> design) throws MalformedURLException {
    System.out.println("update design " + uuid);
    return given().config(restAssuredConfig)
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(design)
            .when().put(makeBaseURL("/a/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private JsonObject createConsumerConfig(String group) {
    final JsonObject config = new JsonObject();
    config.put("kafka_bootstrapServers", System.getProperty("kafka.host", "localhost") + ":9092");
    config.put("kafka_group_id", group);
    return config;
  }

  private Map<String, String> createPostData(String script) {
    final Map<String, String> data = new HashMap<>();
    data.put("manifest", MANIFEST);
    data.put("metadata", METADATA);
    data.put("script", script);
    return data;
  }
}
