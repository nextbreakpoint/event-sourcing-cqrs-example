package com.nextbreakpoint.blueprint.designs;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.nextbreakpoint.blueprint.common.core.Authority;
import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.core.Headers;
import com.nextbreakpoint.blueprint.common.vertx.CassandraClientFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.cassandra.CassandraClient;
import io.vertx.rxjava.core.Vertx;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import rx.Single;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PactTests {
  private static final String SCRIPT1 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String SCRIPT2 = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 100] (mod2(x) > 30) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";
  private static final UUID DESIGN_UUID_1 = new UUID(1L, 1L);
  private static final UUID DESIGN_UUID_2 = new UUID(1L, 2L);

  private static final TestScenario scenario = new TestScenario();

  private static Environment environment = Environment.getDefaultEnvironment();

  private static CassandraClient session;

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    scenario.before();

    System.setProperty("pact.verifier.publishResults", "true");
    System.setProperty("pact.provider.version", scenario.getVersion());

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
  @Tag("pact")
  @DisplayName("Verify contract between designs-aggregate-fetcher and frontend")
  @Provider("designs-aggregate-fetcher")
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
      final String authorization = scenario.makeAuthorization("test", Authority.GUEST);
      request.setHeader(Headers.AUTHORIZATION, authorization);
      context.verifyInteraction();
    }

    @State("there are some designs")
    public void designsExist() {
      session.rxPrepare("TRUNCATE DESIGN_ENTITY")
              .map(PreparedStatement::bind)
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      final String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();
      final String json2 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT2)).toString();

      final Single<PreparedStatement> preparedStatementSingle = session.rxPrepare("INSERT INTO DESIGN_ENTITY (DESIGN_UUID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_CREATED, DESIGN_UPDATED) VALUES (?,?,?,toTimeStamp(now()),toTimeStamp(now()))");

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_1, json1, "1"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_2, json2, "1"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();
    }

    @State("design exists for uuid")
    public void designExistsForUuid() {
      session.rxPrepare("TRUNCATE DESIGN_ENTITY")
              .map(PreparedStatement::bind)
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();

      final String json1 = new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT1)).toString();

      final Single<PreparedStatement> preparedStatementSingle = session.rxPrepare("INSERT INTO DESIGN_ENTITY (DESIGN_UUID, DESIGN_DATA, DESIGN_CHECKSUM, DESIGN_CREATED, DESIGN_UPDATED) VALUES (?,?,?,toTimeStamp(now()),toTimeStamp(now()))");

      preparedStatementSingle
              .map(stmt -> stmt.bind(DESIGN_UUID_1, json1, "1"))
              .flatMap(session::rxExecute)
              .toBlocking()
              .value();
    }
  }

  private static Map<String, Object> createPostData(String manifest, String metadata, String script) {
    final Map<String, Object> data = new HashMap<>();
    data.put("manifest", manifest);
    data.put("metadata", metadata);
    data.put("script", script);
    return data;
  }
}
