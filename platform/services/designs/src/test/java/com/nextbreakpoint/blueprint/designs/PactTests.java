package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.common.core.Message;
import com.nextbreakpoint.blueprint.common.test.KafkaUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class PactTests {
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

    System.setProperty("pact.verifier.publishResults", "true");
    System.setProperty("pact.provider.version", scenario.getVersion());

    consumer = KafkaUtils.createConsumer(environment, scenario.createConsumerConfig("test"));

    consumer.subscribe(Collections.singleton("designs-sse"));

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
  @Tag("pact")
  @DisplayName("Verify contract between designs and frontend")
  @Provider("designs")
  @Consumer("frontend")
  @PactBroker
  public class VerifyFrontend {
    @BeforeEach
    public void before(PactVerificationContext context) {
      context.setTarget(new HttpsTestTarget(scenario.getServiceHost(), Integer.parseInt(scenario.getServicePort()), "/", true));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    @DisplayName("Verify interaction")
    public void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
      final String authorization = scenario.makeAuthorization("test", Authority.ADMIN);
      request.setHeader(Headers.AUTHORIZATION, authorization);
      context.verifyInteraction();
    }

    @State("there are no designs")
    public void designsTableExists() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("designs"), "root", "password")) {
        connection.prepareStatement("TRUNCATE DESIGNS;").execute();
      }
    }

    @State("there are some designs")
    public void designsExist() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("designs"), "root", "password")) {
        connection.prepareStatement("TRUNCATE DESIGNS;").execute();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO DESIGNS (UUID,JSON,CHECKSUM,CREATED,UPDATED) VALUES (?,?,?,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP());");
        String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
        statement.setString(1, DESIGN_UUID_1.toString());
        statement.setString(2, json1);
        statement.setString(3, "1");
        statement.execute();
        String json2 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT2)).toString();
        statement.setString(1, DESIGN_UUID_2.toString());
        statement.setString(2, json2);
        statement.setString(3, "2");
        statement.execute();
      }
    }

    @State("design exists for uuid")
    public void designExistsForUuid() throws SQLException {
      try (Connection connection = DriverManager.getConnection(scenario.getMySqlConnectionUrl("designs"), "root", "password")) {
        connection.prepareStatement("TRUNCATE DESIGNS;").execute();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO DESIGNS (UUID,JSON,CHECKSUM,CREATED,UPDATED) VALUES (?,?,?,CURRENT_TIMESTAMP(),CURRENT_TIMESTAMP());");
        String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
        statement.setString(1, DESIGN_UUID_1.toString());
        statement.setString(2, json1);
        statement.setString(3, "1");
        statement.execute();
      }
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
          consumerRecords.forEach(PactTests::safelyAppendRecord);
          consumer.commitSync();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private static Map<String, Object> createPostData(String manifest, String metadata, String script) {
    final Map<String, Object> data = new HashMap<>();
    data.put("manifest", manifest);
    data.put("metadata", metadata);
    data.put("script", script);
    return data;
  }
}
