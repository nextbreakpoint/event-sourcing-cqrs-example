package com.nextbreakpoint.blueprint.designs;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.DesignDocument;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.core.event.DesignChanged;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import io.vertx.core.json.Json;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;
import static com.nextbreakpoint.blueprint.common.core.Headers.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.hamcrest.CoreMatchers.notNullValue;

public class IntegrationTests {
  private static final String SCRIPT1 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String SCRIPT2 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 100] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";
  private static final UUID DESIGN_UUID_1 = new UUID(1L, 1L);
  private static final UUID DESIGN_UUID_2 = new UUID(1L, 2L);

  private static final TestScenario scenario = new TestScenario();

  private static Environment environment = Environment.getDefaultEnvironment();

  private static final List<ConsumerRecord<String, String>> records = new ArrayList<>();
  private static KafkaConsumer<String, String> consumer;
  private static Thread polling;

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();

    consumer = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test"));

    consumer.subscribe(Collections.singleton("design-event"));

    polling = createConsumerThread();

    polling.start();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    if (polling != null) {
      try {
        polling.interrupt();
        polling.join();
      } catch (Exception ignore) {
      }
    }

    if (consumer != null) {
      try {
        consumer.close();
      } catch (Exception ignore) {
      }
    }

    scenario.after();
  }

  @Nested
  @Tag("slow")
  @Tag("integration")
  @DisplayName("Verify behaviour of designs service")
  public class VerifyServiceApi {
    @BeforeEach
    public void setup() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("designs"), "root", "password")) {
        connection.prepareStatement("TRUNCATE DESIGN_ENTITY;").execute();
      }
    }

    @AfterEach
    public void reset() {
      RestAssured.reset();
    }

    @Test
    @DisplayName("should allow options request without access token")
    public void shouldAllowOptionsRequestWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().options(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");

      given().config(scenario.getRestAssuredConfig())
              .with().header("Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .when().options(scenario.makeBaseURL("/v1/designs/" + UUID.randomUUID().toString()))
              .then().assertThat().statusCode(204)
              .and().header("Access-Control-Allow-Origin", "https://" + scenario.getServiceHost() + ":" + scenario.getServicePort())
              .and().header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("should forbid post request without access token")
    public void shouldForbidPostRequestWithoutAccessToken() throws MalformedURLException {
      given().config(scenario.getRestAssuredConfig())
              .and().contentType(ContentType.JSON)
              .and().accept(ContentType.JSON)
              .and().body(createPostData(MANIFEST, METADATA, SCRIPT1))
              .when().post(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should forbid delete request without access token")
    public void shouldForbidDeleteRequestWithoutAccessToken() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String uuid = createDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT1));

      given().config(scenario.getRestAssuredConfig())
              .and().accept(ContentType.JSON)
              .when().delete(scenario.makeBaseURL("/v1/designs/" + uuid))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should forbid get request when user doesn't have permissions")
    public void shouldForbidGetRequestWhenUserDoNotHavePermissions() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String uuid = createDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT1));

      final String otherAuthorization = scenario.makeAuthorization("test", "other");

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(403);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs/" + uuid))
              .then().assertThat().statusCode(403);

      given().config(scenario.getRestAssuredConfig())
              .with().header(AUTHORIZATION, otherAuthorization)
              .and().accept("image/png")
              .when().get(scenario.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
              .then().assertThat().statusCode(403);
    }

    @Test
    @DisplayName("should allow get request when user is anonymous")
    public void shouldAllowGetRequestWhenUserIsAnonymous() throws MalformedURLException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      final String uuid = createDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT1));

      pause();

      given().config(scenario.getRestAssuredConfig())
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs"))
              .then().assertThat().statusCode(200);

      given().config(scenario.getRestAssuredConfig())
              .and().accept(ContentType.JSON)
              .when().get(scenario.makeBaseURL("/v1/designs/" + uuid))
              .then().assertThat().statusCode(200);

      given().config(scenario.getRestAssuredConfig())
              .and().accept("image/png")
              .when().get(scenario.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
              .then().assertThat().statusCode(200);
    }

    @Test
    @DisplayName("should create and delete designs")
    public void shouldCreateAndDeleteDesigns() throws IOException, InterruptedException {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);

      pause();

      long eventTimestamp0 = System.currentTimeMillis();

      safelyClearMessages();

      final String designId = createDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT1));

      await().atMost(TEN_SECONDS)
              .pollInterval(ONE_SECOND)
              .untilAsserted(() -> {
                final Optional<Message> message = safelyFindMessage(UUID.fromString(designId));
                assertThat(message.isEmpty()).isFalse();
                Message actualMessage = message.get();
                assertThat(actualMessage.getTimestamp()).isNotNull();
                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                assertThat(actualMessage.getPartitionKey()).isEqualTo(designId);
                assertThat(actualMessage.getMessageId()).isNotNull();
                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(designId));
                assertThat(actualEvent.getTimestamp()).isNotNull();
                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp0);
              });

      safelyClearMessages();

      pause();

      final JsonPath jsonPath0 = getDesign(authorization, designId);

      assertThat(jsonPath0.getString("uuid")).isEqualTo(designId);
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

      updateDesign(authorization, designId, createPostData(MANIFEST, METADATA, SCRIPT2));

      pause();

      await().atMost(TEN_SECONDS)
              .pollInterval(ONE_SECOND)
              .untilAsserted(() -> {
                final Optional<Message> message = safelyFindMessage(UUID.fromString(designId));
                assertThat(message.isEmpty()).isFalse();
                Message actualMessage = message.get();
                assertThat(actualMessage.getTimestamp()).isNotNull();
                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                assertThat(actualMessage.getPartitionKey()).isEqualTo(designId);
                assertThat(actualMessage.getMessageId()).isNotNull();
                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(designId));
                assertThat(actualEvent.getTimestamp()).isNotNull();
                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp1);
              });

      safelyClearMessages();

      final JsonPath jsonPath1 = getDesign(authorization, designId);

      assertThat(jsonPath1.getString("uuid")).isEqualTo(designId);
      assertThat(jsonPath1.getString("json")).isNotNull();
      assertThat(jsonPath1.getString("modified")).isNotNull();
      assertThat(jsonPath1.getString("checksum")).isNotNull();

      final String json1 = jsonPath1.getString("json");
      System.out.println(json1);

      final Map<String, Object> object1 = Json.decodeValue(json1, HashMap.class);
      assertThat(object1.get("script")).isEqualTo(SCRIPT2);
      assertThat(object1.get("metadata")).isEqualTo(METADATA);
      assertThat(object1.get("manifest")).isEqualTo(MANIFEST);

      final String uuid2 = createDesign(authorization, createPostData(MANIFEST, METADATA, SCRIPT1));

      pause();

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

      final DesignDocument document1 = new DesignDocument(designId, null, jsonPath1.getString("checksum"), null);
      final DesignDocument document2 = new DesignDocument(uuid2, null, jsonPath2.getString("checksum"), null);

      assertThat(getDesigns(authorization)).contains(document1, document2);

      final byte[] bytes = getTile(authorization, designId);

      final BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      assertThat(image.getWidth()).isEqualTo(256);
      assertThat(image.getHeight()).isEqualTo(256);

      long eventTimestamp3 = System.currentTimeMillis();

      deleteDesign(authorization, designId);

      pause();

      await().atMost(TEN_SECONDS)
              .pollInterval(ONE_SECOND)
              .untilAsserted(() -> {
                final Optional<Message> message = safelyFindMessage(UUID.fromString(designId));
                assertThat(message.isEmpty()).isFalse();
                Message actualMessage = message.get();
                assertThat(actualMessage.getTimestamp()).isNotNull();
                assertThat(actualMessage.getMessageSource()).isEqualTo("service-designs");
                assertThat(actualMessage.getPartitionKey()).isEqualTo(designId);
                assertThat(actualMessage.getMessageId()).isNotNull();
                assertThat(actualMessage.getMessageType()).isEqualTo("design-changed");
                DesignChanged actualEvent = Json.decodeValue(actualMessage.getMessageBody(), DesignChanged.class);
                assertThat(actualEvent.getUuid()).isEqualTo(UUID.fromString(designId));
                assertThat(actualEvent.getTimestamp()).isNotNull();
                assertThat(actualEvent.getTimestamp()).isGreaterThan(eventTimestamp3);
              });

      safelyClearMessages();

      assertThat(getDesigns(authorization)).contains(document2);
      assertThat(getDesigns(authorization)).doesNotContain(document1);

      deleteDesign(authorization, uuid2);

      pause();

      assertThat(getDesigns(authorization)).doesNotContain(document1, document2);
    }
  }

  private static void pause() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ignored) {
    }
  }

  private static Optional<Message> safelyFindMessage(UUID designId) {
    synchronized (records) {
      return records.stream()
              .map(record -> Json.decodeValue(record.value(), Message.class))
              .filter(value -> value.getPartitionKey().equals(designId.toString()))
              .findFirst();
    }
  }

  private static void safelyClearMessages() {
    synchronized (records) {
      records.clear();
    }
  }

  private static void safelyAppendRecord(ConsumerRecord<String, String> record) {
    synchronized (records) {
      records.add(record);
    }
  }

  private static Thread createConsumerThread() {
    return new Thread(() -> {
      try {
        while (!Thread.interrupted()) {
          ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(5));
          System.out.println("Received " + consumerRecords.count() + " messages");
          consumerRecords.forEach(IntegrationTests::safelyAppendRecord);
          consumer.commitSync();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private static byte[] getTile(String authorization, String uuid) throws MalformedURLException {
    return given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept("image/png")
            .when().get(scenario.makeBaseURL("/v1/designs/" + uuid + "/0/0/0/256.png"))
            .then().assertThat().statusCode(200)
            .and().contentType("image/png")
            .and().extract().asByteArray();
  }

  private static DesignDocument[] getDesigns(String authorization) throws MalformedURLException {
    System.out.println("get designs");
    return given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(scenario.makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().extract().body().as(DesignDocument[].class);
  }

  private static JsonPath getDesign(String authorization, String uuid) throws MalformedURLException {
    System.out.println("get design " + uuid);
    return given().config(scenario.getRestAssuredConfig())
            .with().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().get(scenario.makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200).extract().jsonPath();
  }

  private static String createDesign(String authorization, Map<String, Object> design) throws MalformedURLException {
    System.out.println("create design");
    return given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(design)
            .when().post(scenario.makeBaseURL("/v1/designs"))
            .then().assertThat().statusCode(201)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private static String updateDesign(String authorization, String uuid, Map<String, Object> design) throws MalformedURLException {
    System.out.println("update design " + uuid);
    return given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().contentType(ContentType.JSON)
            .and().accept(ContentType.JSON)
            .and().body(design)
            .when().put(scenario.makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue())
            .and().extract().response().body().jsonPath().getString("uuid");
  }

  private static void deleteDesign(String authorization, String uuid) throws MalformedURLException {
    System.out.println("delete design " + uuid);
    given().config(scenario.getRestAssuredConfig())
            .and().header(AUTHORIZATION, authorization)
            .and().accept(ContentType.JSON)
            .when().delete(scenario.makeBaseURL("/v1/designs/" + uuid))
            .then().assertThat().statusCode(200)
            .and().contentType(ContentType.JSON)
            .and().body("uuid", notNullValue());
  }

  private static Map<String, Object> createPostData(String manifest, String metadata, String script) {
    final Map<String, Object> data = new HashMap<>();
    data.put("manifest", manifest);
    data.put("metadata", metadata);
    data.put("script", script);
    return data;
  }
}
