package com.nextbreakpoint.blueprint.frontend;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.jayway.jsonpath.JsonPath;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Tag("pact")
@DisplayName("Test frontend pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
  private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
  private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
  private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";
  private static final UUID DESIGN_UUID_1 = new UUID(1L, 1L);
  private static final UUID DESIGN_UUID_2 = new UUID(1L, 2L);
  private static final UUID ACCOUNT_UUID = new UUID(1L, 1L);

  private static final TestScenario scenario = new TestScenario();

  @BeforeAll
  public static void before() throws IOException, InterruptedException {
    System.setProperty("http.port", "30400");
    System.setProperty("stub.port", "39001");

    scenario.before();
  }

  @AfterAll
  public static void after() throws IOException, InterruptedException {
    scenario.after();
  }

  @Pact(consumer = "frontend")
  public RequestResponsePact retrieveAccount(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("account exists for uuid")
            .uponReceiving("request to fetch account")
            .method("GET")
            .path("/v1/accounts/" + ACCOUNT_UUID.toString())
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonBody()
                            .stringValue("uuid", ACCOUNT_UUID.toString())
                            .stringValue("role", "guest")
            )
            .toPact();
  }

  @Pact(consumer = "frontend")
  public RequestResponsePact retrieveDesigns(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("there are some designs")
            .uponReceiving("request to retrieve designs")
            .method("GET")
            .path("/v1/designs")
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonArray()
                            .object()
                            .stringValue("uuid", DESIGN_UUID_1.toString())
                            .stringMatcher("checksum", ".+")
                            .closeObject()
                            .object()
                            .stringValue("uuid", DESIGN_UUID_2.toString())
                            .stringMatcher("checksum", ".+")
                            .closeObject()
            )
            .toPact();
  }

//  @Pact(consumer = "frontend")
//  public RequestResponsePact retrieveDesign(PactDslWithProvider builder) {
//    final Map<String, String> headers = new HashMap<>();
//    headers.put("Content-Type", "application/json");
//    return builder
//            .given("design exists for uuid")
//            .uponReceiving("request to fetch design")
//            .method("GET")
//            .path("/v1/designs/" + DESIGN_UUID_1.toString())
//            .matchHeader("Accept", "application/json")
//            .matchHeader("Authorization", "Bearer .+")
//            .willRespondWith()
//            .headers(headers)
//            .status(200)
//            .body(
//                    new PactDslJsonBody()
//                            .stringValue("uuid", DESIGN_UUID_1.toString())
//                            .stringMatcher("json", ".+")
//                            .stringMatcher("checksum", ".+")
//                            .timestamp("modified", "yyyy-MM-dd'T'HH:mm:ss'Z'")
//            )
//            .toPact();
//  }
//
//  @Pact(consumer = "frontend")
//  public RequestResponsePact insertDesign(PactDslWithProvider builder) {
//    final Map<String, String> headers = new HashMap<>();
//    headers.put("Content-Type", "application/json");
//    return builder
//            .given("there are no designs")
//            .uponReceiving("request to insert design")
//            .method("POST")
//            .path("/v1/designs")
//            .matchHeader("Accept", "application/json")
//            .matchHeader("Content-Type", "application/json")
//            .matchHeader("Authorization", "Bearer .+")
//            .body(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString())
//            .willRespondWith()
//            .headers(headers)
//            .status(201)
//            .body(
//                    new PactDslJsonBody()
//                            .stringMatcher("uuid", ".+")
//            )
//            .toPact();
//  }
//
//  @Pact(consumer = "frontend")
//  public RequestResponsePact updateDesign(PactDslWithProvider builder) {
//    final Map<String, String> headers = new HashMap<>();
//    headers.put("Content-Type", "application/json");
//    return builder
//            .given("design exists for uuid")
//            .uponReceiving("request to update design")
//            .method("PUT")
//            .path("/v1/designs/" + DESIGN_UUID_1)
//            .matchHeader("Accept", "application/json")
//            .matchHeader("Content-Type", "application/json")
//            .matchHeader("Authorization", "Bearer .+")
//            .body(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString())
//            .willRespondWith()
//            .headers(headers)
//            .status(200)
//            .body(
//                    new PactDslJsonBody()
//                            .stringValue("uuid", DESIGN_UUID_1.toString())
//            )
//            .toPact();
//  }
//
//  @Pact(consumer = "frontend")
//  public RequestResponsePact deleteDesign(PactDslWithProvider builder) {
//    final Map<String, String> headers = new HashMap<>();
//    headers.put("Content-Type", "application/json");
//    return builder
//            .given("design exists for uuid")
//            .uponReceiving("request to delete design")
//            .method("DELETE")
//            .path("/v1/designs/" + DESIGN_UUID_1)
//            .matchHeader("Accept", "application/json")
//            .matchHeader("Authorization", "Bearer .+")
//            .willRespondWith()
//            .headers(headers)
//            .status(200)
//            .body(
//                    new PactDslJsonBody()
//                            .stringValue("uuid", DESIGN_UUID_1.toString())
//            )
//            .toPact();
//  }

  @Pact(consumer = "frontend")
  public RequestResponsePact retrieveDesignWhenUsingCQRS(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("design exists for uuid")
            .uponReceiving("request to fetch design")
            .method("GET")
            .path("/v1/designs/" + DESIGN_UUID_1.toString())
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .willRespondWith()
            .headers(headers)
            .status(200)
            .body(
                    new PactDslJsonBody()
                            .stringValue("uuid", DESIGN_UUID_1.toString())
                            .stringMatcher("json", ".+")
                            .stringMatcher("checksum", ".+")
                            .timestamp("modified", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            )
            .toPact();
  }

  @Pact(consumer = "frontend")
  public RequestResponsePact insertDesignWhenUsingCQRS(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("kafka topic exists")
            .uponReceiving("request to insert design")
            .method("POST")
            .path("/v1/designs")
            .matchHeader("Accept", "application/json")
            .matchHeader("Content-Type", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .body(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString())
            .willRespondWith()
            .headers(headers)
            .status(202)
            .body(
                    new PactDslJsonBody()
                            .stringMatcher("uuid", ".+")
                            .stringMatcher("status", ".+")
            )
            .toPact();
  }

  @Pact(consumer = "frontend")
  public RequestResponsePact updateDesignWhenUsingCQRS(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("kafka topic exists")
            .uponReceiving("request to update design")
            .method("PUT")
            .path("/v1/designs/" + DESIGN_UUID_1)
            .matchHeader("Accept", "application/json")
            .matchHeader("Content-Type", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .body(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString())
            .willRespondWith()
            .headers(headers)
            .status(202)
            .body(
                    new PactDslJsonBody()
                            .stringValue("uuid", DESIGN_UUID_1.toString())
                            .stringMatcher("status", ".+")
            )
            .toPact();
  }

  @Pact(consumer = "frontend")
  public RequestResponsePact deleteDesignWhenUsingCQRS(PactDslWithProvider builder) {
    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return builder
            .given("kafka topic exists")
            .uponReceiving("request to delete design")
            .method("DELETE")
            .path("/v1/designs/" + DESIGN_UUID_1)
            .matchHeader("Accept", "application/json")
            .matchHeader("Authorization", "Bearer .+")
            .willRespondWith()
            .headers(headers)
            .status(202)
            .body(
                    new PactDslJsonBody()
                            .stringValue("uuid", DESIGN_UUID_1.toString())
                            .stringMatcher("status", ".+")
            )
            .toPact();
  }

  @Test
  @PactTestFor(providerName = "accounts", port = "1110", pactMethod = "retrieveAccount")
  public void shouldRetrieveAccount(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/accounts/" + ACCOUNT_UUID)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer abcdef")
            .execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(ACCOUNT_UUID.toString());
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.role").toString()).isEqualTo("guest");
  }

//  @Test
//  @PactTestFor(providerName = "designs", port = "1111", pactMethod = "retrieveDesigns")
//  public void shouldRetrieveDesigns(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs")
//            .addHeader("Accept", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0].uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0].checksum").toString()).isNotBlank();
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[1].uuid").toString()).isEqualTo(DESIGN_UUID_2.toString());
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[1].checksum").toString()).isNotBlank();
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", port = "1112", pactMethod = "retrieveDesign")
//  public void shouldRetrieveDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1.toString())
//            .addHeader("Accept", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.json").toString()).isNotBlank();
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.checksum").toString()).isNotBlank();
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.modified").toString()).isNotBlank();
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", port = "1113", pactMethod = "insertDesign")
//  public void shouldInsertDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Post(mockServer.getUrl() + "/v1/designs")
//            .addHeader("Accept", "application/json")
//            .addHeader("Content-Type", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .body(new StringEntity(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString()))
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(201);
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isNotBlank();
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", port = "1114", pactMethod = "updateDesign")
//  public void shouldUpdateDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Put(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
//            .addHeader("Accept", "application/json")
//            .addHeader("Content-Type", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .body(new StringEntity(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString()))
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", port = "1115", pactMethod = "deleteDesign")
//  public void shouldDeleteDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Delete(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
//            .addHeader("Accept", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//  }

  @Test
  @PactTestFor(providerName = "designs-aggregate-fetcher", port = "1116", pactMethod = "retrieveDesigns")
  public void shouldRetrieveDesignsWhenUsingCQRS(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer abcdef")
            .execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0].uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[0].checksum").toString()).isNotBlank();
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[1].uuid").toString()).isEqualTo(DESIGN_UUID_2.toString());
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.[1].checksum").toString()).isNotBlank();
  }

  @Test
  @PactTestFor(providerName = "designs-aggregate-fetcher", port = "1117", pactMethod = "retrieveDesignWhenUsingCQRS")
  public void shouldRetrieveDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer abcdef")
            .execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.json").toString()).isNotBlank();
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.checksum").toString()).isNotBlank();
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.modified").toString()).isNotBlank();
  }

  @Test
  @PactTestFor(providerName = "designs-command-producer", port = "1118", pactMethod = "insertDesignWhenUsingCQRS")
  public void shouldInsertDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Post(mockServer.getUrl() + "/v1/designs")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer abcdef")
            .body(new StringEntity(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString()))
            .execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(202);
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isNotBlank();
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.status").toString()).isNotBlank();
  }

  @Test
  @PactTestFor(providerName = "designs-command-producer", port = "1119", pactMethod = "updateDesignWhenUsingCQRS")
  public void shouldUpdateDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Put(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer abcdef")
            .body(new StringEntity(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString()))
            .execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(202);
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.status").toString()).isNotBlank();
  }

  @Test
  @PactTestFor(providerName = "designs-command-producer", port = "1120", pactMethod = "deleteDesignWhenUsingCQRS")
  public void shouldDeleteDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Delete(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer abcdef")
            .execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(202);
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
    assertThat(JsonPath.read(httpResponse.getEntity().getContent(), "$.status").toString()).isNotBlank();
  }

  public static Map<String, Object> createPostData(String manifest, String metadata, String script) {
    final Map<String, Object> data = new HashMap<>();
    data.put("manifest", manifest);
    data.put("metadata", metadata);
    data.put("script", script);
    return data;
  }
}
