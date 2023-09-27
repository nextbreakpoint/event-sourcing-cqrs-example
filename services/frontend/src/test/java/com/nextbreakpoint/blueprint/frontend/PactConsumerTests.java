package com.nextbreakpoint.blueprint.frontend;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.vertx.core.json.JsonObject;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("docker")
@Tag("pact")
@DisplayName("Test frontend pact")
@ExtendWith(PactConsumerTestExt.class)
public class PactConsumerTests {
    private static final TestScenario scenario = new TestScenario();

    @BeforeAll
    public static void before() {
        scenario.before();
    }

    @AfterAll
    public static void after() {
        scenario.after();
    }

    @Pact(consumer = "frontend")
    public V4Pact retrieveAccount(PactBuilder builder) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder.usingLegacyDsl()
                .given("account exists for uuid")
                .uponReceiving("request to fetch account")
                .method("GET")
                .path("/v1/accounts/" + TestConstants.ACCOUNT_UUID)
                .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
                .matchHeader("Accept", "application/json", "application/json")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body(
                        new PactDslJsonBody()
                                .stringValue("uuid", TestConstants.ACCOUNT_UUID.toString())
                                .stringValue("role", "guest")
                )
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "frontend")
    public V4Pact retrieveDesigns(PactBuilder builder) {
        final String json = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT)).toString();
        final String checksum = "0001";
        final String revision = "0000000000000000-0000000000000001";
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder.usingLegacyDsl()
                .given("there are some designs")
                .uponReceiving("request to retrieve designs")
                .method("GET")
                .path("/v1/designs")
                .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
                .matchHeader("Accept", "application/json", "application/json")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body(
                        new PactDslJsonBody()
                                .array("designs")
                                    .object()
                                        .stringValue("uuid", TestConstants.DESIGN_UUID_2.toString())
                                        .stringMatcher("checksum", ".+", checksum)
                                        .stringMatcher("revision", ".+", revision)
                                        .stringMatcher("json", ".+", json)
                                        .datetime("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                        .datetime("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                        .booleanValue("published", false)
                                        .integerType("levels", 3)
                                        .array("tiles")
                                            .object()
                                                .integerType("level", 0)
                                                .integerType("total", 1)
                                                .integerType("completed", 0)
                                                .closeObject()
                                            .object()
                                                .integerType("level", 1)
                                                .integerType("total", 4)
                                                .integerType("completed", 0)
                                                .closeObject()
                                            .object()
                                                .integerType("level", 2)
                                                .integerType("total", 16)
                                                .integerType("completed", 0)
                                                .closeObject()
                                            .closeArray()
                                        .closeObject()
                                    .object()
                                        .stringValue("uuid", TestConstants.DESIGN_UUID_1.toString())
                                        .stringMatcher("checksum", ".+", checksum)
                                        .stringMatcher("revision", ".+", revision)
                                        .stringMatcher("json", ".+", json)
                                        .datetime("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                        .datetime("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                        .booleanValue("published", false)
                                        .integerType("levels", 3)
                                        .array("tiles")
                                            .object()
                                                .integerType("level", 0)
                                                .integerType("total", 1)
                                                .integerType("completed", 0)
                                                .closeObject()
                                            .object()
                                                .integerType("level", 1)
                                                .integerType("total", 4)
                                                .integerType("completed", 0)
                                                .closeObject()
                                            .object()
                                                .integerType("level", 2)
                                                .integerType("total", 16)
                                                .integerType("completed", 0)
                                                .closeObject()
                                            .closeArray()
                                        .closeObject()
                                    .closeArray()
                )
                .toPact(V4Pact.class);
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
    public V4Pact retrieveDesignWhenUsingCQRS(PactBuilder builder) {
        final String json = new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT)).toString();
        final String checksum = "0001";
        final String revision = "0000000000000000-0000000000000001";
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder.usingLegacyDsl()
                .given("design exists for uuid")
                .uponReceiving("request to fetch design")
                .method("GET")
                .path("/v1/designs/" + TestConstants.DESIGN_UUID_1)
                .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
                .matchHeader("Accept", "application/json", "application/json")
                .willRespondWith()
                .headers(headers)
                .status(200)
                .body(
                        new PactDslJsonBody()
                                .stringValue("uuid", TestConstants.DESIGN_UUID_1.toString())
                                .stringMatcher("checksum", ".+", checksum)
                                .stringMatcher("revision", ".+", revision)
                                .stringMatcher("json", ".+", json)
                                .datetime("created", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                .datetime("updated", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                .booleanValue("published", false)
                                .integerType("levels", 3)
                                .array("tiles")
                                    .object()
                                        .integerType("level", 0)
                                        .integerType("total", 1)
                                        .integerType("completed", 0)
                                        .closeObject()
                                    .object()
                                        .integerType("level", 1)
                                        .integerType("total", 4)
                                        .integerType("completed", 0)
                                        .closeObject()
                                    .object()
                                        .integerType("level", 2)
                                        .integerType("total", 16)
                                        .integerType("completed", 0)
                                        .closeObject()
                                    .closeArray()
                )
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "frontend")
    public V4Pact insertDesignWhenUsingCQRS(PactBuilder builder) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder.usingLegacyDsl()
                .given("kafka topic exists")
                .uponReceiving("request to insert design")
                .method("POST")
                .path("/v1/designs")
                .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
                .matchHeader("Accept", "application/json", "application/json")
                .matchHeader("Content-Type", "application/json", "application/json")
                .body(new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT)).toString())
                .willRespondWith()
                .headers(headers)
                .status(202)
                .body(
                        new PactDslJsonBody()
                                .stringMatcher("uuid", ".+", TestConstants.DESIGN_UUID_1.toString())
                                .stringMatcher("status", ".+", "CREATED")
                )
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "frontend")
    public V4Pact updateDesignWhenUsingCQRS(PactBuilder builder) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder.usingLegacyDsl()
                .given("kafka topic exists")
                .uponReceiving("request to update design")
                .method("PUT")
                .path("/v1/designs/" + TestConstants.DESIGN_UUID_1)
                .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
                .matchHeader("Accept", "application/json", "application/json")
                .matchHeader("Content-Type", "application/json", "application/json")
                .body(new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT)).toString())
                .willRespondWith()
                .headers(headers)
                .status(202)
                .body(
                        new PactDslJsonBody()
                                .stringValue("uuid", TestConstants.DESIGN_UUID_1.toString())
                                .stringMatcher("status", ".+", "UPDATED")
                )
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "frontend")
    public V4Pact deleteDesignWhenUsingCQRS(PactBuilder builder) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return builder.usingLegacyDsl()
                .given("kafka topic exists")
                .uponReceiving("request to delete design")
                .method("DELETE")
                .path("/v1/designs/" + TestConstants.DESIGN_UUID_1)
                .matchHeader("Authorization", "Bearer .+", "Bearer abcdef")
                .matchHeader("Accept", "application/json", "application/json")
                .willRespondWith()
                .headers(headers)
                .status(202)
                .body(
                        new PactDslJsonBody()
                                .stringValue("uuid", TestConstants.DESIGN_UUID_1.toString())
                                .stringMatcher("status", ".+", "DELETED")
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(providerName = "accounts", pactMethod = "retrieveAccount", pactVersion = PactSpecVersion.V4)
    @MockServerConfig(providerName = "accounts", port = "1110")
    public void shouldRetrieveAccount(MockServer mockServer) throws IOException {
        Request.get(mockServer.getUrl() + "/v1/accounts/" + TestConstants.ACCOUNT_UUID)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer abcdef")
                .execute()
                .handleResponse(httpResponse -> {
                    assertThat(httpResponse.getCode()).isEqualTo(200);
                    final DocumentContext content = JsonPath.parse(httpResponse.getEntity().getContent());
                    assertThat(content.read("$.uuid").toString()).isEqualTo(TestConstants.ACCOUNT_UUID.toString());
                    assertThat(content.read("$.role").toString()).isEqualTo("guest");
                    return null;
                });
    }

//  @Test
//  @PactTestFor(providerName = "designs", pactMethod = "retrieveDesigns")
//  public void shouldRetrieveDesigns(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs")
//            .addHeader("Accept", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(content, "$.[0].uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//    assertThat(JsonPath.read(content, "$.[0].checksum").toString()).isNotBlank();
//    assertThat(JsonPath.read(content, "$.[1].uuid").toString()).isEqualTo(DESIGN_UUID_2.toString());
//    assertThat(JsonPath.read(content, "$.[1].checksum").toString()).isNotBlank();
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", pactMethod = "retrieveDesign")
//  public void shouldRetrieveDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1.toString())
//            .addHeader("Accept", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(content, "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//    assertThat(JsonPath.read(content, "$.json").toString()).isNotBlank();
//    assertThat(JsonPath.read(content, "$.checksum").toString()).isNotBlank();
//    assertThat(JsonPath.read(content, "$.modified").toString()).isNotBlank();
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", pactMethod = "insertDesign")
//  public void shouldInsertDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Post(mockServer.getUrl() + "/v1/designs")
//            .addHeader("Accept", "application/json")
//            .addHeader("Content-Type", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .body(new StringEntity(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString()))
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(201);
//    assertThat(JsonPath.read(content, "$.uuid").toString()).isNotBlank();
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", pactMethod = "updateDesign")
//  public void shouldUpdateDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Put(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
//            .addHeader("Accept", "application/json")
//            .addHeader("Content-Type", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .body(new StringEntity(new JsonObject(createPostData(MANIFEST, METADATA, SCRIPT)).toString()))
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(content, "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//  }
//
//  @Test
//  @PactTestFor(providerName = "designs", pactMethod = "deleteDesign")
//  public void shouldDeleteDesign(MockServer mockServer) throws IOException {
//    HttpResponse httpResponse = Request.Delete(mockServer.getUrl() + "/v1/designs/" + DESIGN_UUID_1)
//            .addHeader("Accept", "application/json")
//            .addHeader("Authorization", "Bearer abcdef")
//            .execute().returnResponse();
//    assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
//    assertThat(JsonPath.read(content, "$.uuid").toString()).isEqualTo(DESIGN_UUID_1.toString());
//  }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "retrieveDesigns", pactVersion = PactSpecVersion.V4)
    @MockServerConfig(providerName = "designs-query", port = "1116")
    public void shouldRetrieveDesignsWhenUsingCQRS(MockServer mockServer) throws IOException {
        Request.get(mockServer.getUrl() + "/v1/designs")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer abcdef")
                .execute()
                .handleResponse(httpResponse -> {
                    assertThat(httpResponse.getCode()).isEqualTo(200);
                    final DocumentContext content = JsonPath.parse(httpResponse.getEntity().getContent());
                    assertThat(content.read("$.designs[0].uuid").toString()).isEqualTo(TestConstants.DESIGN_UUID_2.toString());
                    assertThat(content.read("$.designs[0].checksum").toString()).isNotBlank();
                    assertThat(content.read("$.designs[0].revision").toString()).isNotBlank();
                    assertThat(content.read("$.designs[0].updated").toString()).isNotBlank();
                    assertThat(content.read("$.designs[0].created").toString()).isNotBlank();
                    assertThat(content.read("$.designs[0].revision").toString()).isNotBlank();
                    assertThat((Integer) content.read("$.designs[0].levels")).isNotNull();
                    assertThat((Boolean) content.read("$.designs[0].published")).isNotNull();
                    assertThat((List) content.read("$.designs[0].tiles")).isNotEmpty();
                    assertThat(content.read("$.designs[1].uuid").toString()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
                    assertThat(content.read("$.designs[1].checksum").toString()).isNotBlank();
                    assertThat(content.read("$.designs[1].revision").toString()).isNotBlank();
                    assertThat(content.read("$.designs[1].updated").toString()).isNotBlank();
                    assertThat(content.read("$.designs[1].created").toString()).isNotBlank();
                    assertThat(content.read("$.designs[1].revision").toString()).isNotBlank();
                    assertThat((Integer) content.read("$.designs[1].levels")).isNotNull();
                    assertThat((Boolean) content.read("$.designs[1].published")).isNotNull();
                    assertThat((List) content.read("$.designs[1].tiles")).isNotEmpty();
                    return null;
                });
    }

    @Test
    @PactTestFor(providerName = "designs-query", pactMethod = "retrieveDesignWhenUsingCQRS", pactVersion = PactSpecVersion.V4)
    @MockServerConfig(providerName = "designs-query", port = "1117")
    public void shouldRetrieveDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
        Request.get(mockServer.getUrl() + "/v1/designs/" + TestConstants.DESIGN_UUID_1)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer abcdef")
                .execute()
                .handleResponse(httpResponse -> {
                    assertThat(httpResponse.getCode()).isEqualTo(200);
                    final DocumentContext content = JsonPath.parse(httpResponse.getEntity().getContent());
                    assertThat(content.read("$.uuid").toString()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
                    assertThat(content.read("$.json").toString()).isNotBlank();
                    assertThat(content.read("$.checksum").toString()).isNotBlank();
                    assertThat(content.read("$.revision").toString()).isNotBlank();
                    assertThat(content.read("$.updated").toString()).isNotBlank();
                    assertThat(content.read("$.created").toString()).isNotBlank();
                    assertThat(content.read("$.revision").toString()).isNotBlank();
                    assertThat((Integer) content.read("$.levels")).isNotNull();
                    assertThat((Boolean) content.read("$.published")).isNotNull();
                    assertThat((List) content.read("$.tiles")).isNotEmpty();
                    return null;
                });
    }

    @Test
    @PactTestFor(providerName = "designs-command", pactMethod = "insertDesignWhenUsingCQRS", pactVersion = PactSpecVersion.V4)
    @MockServerConfig(providerName = "designs-command", port = "1118")
    public void shouldInsertDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
        Request.post(mockServer.getUrl() + "/v1/designs")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer abcdef")
                .body(new StringEntity(new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT)).toString()))
                .execute()
                .handleResponse(httpResponse -> {
                    assertThat(httpResponse.getCode()).isEqualTo(202);
                    final DocumentContext content = JsonPath.parse(httpResponse.getEntity().getContent());
                    assertThat(content.read("$.uuid").toString()).isNotBlank();
                    assertThat(content.read("$.status").toString()).isNotBlank();
                    return null;
                });
    }

    @Test
    @PactTestFor(providerName = "designs-command", pactMethod = "updateDesignWhenUsingCQRS", pactVersion = PactSpecVersion.V4)
    @MockServerConfig(providerName = "designs-command", port = "1119")
    public void shouldUpdateDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
        Request.put(mockServer.getUrl() + "/v1/designs/" + TestConstants.DESIGN_UUID_1)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer abcdef")
                .body(new StringEntity(new JsonObject(TestUtils.createPostData(TestConstants.MANIFEST, TestConstants.METADATA, TestConstants.SCRIPT)).toString()))
                .execute()
                .handleResponse(httpResponse -> {
                    assertThat(httpResponse.getCode()).isEqualTo(202);
                    final DocumentContext content = JsonPath.parse(httpResponse.getEntity().getContent());
                    assertThat(content.read("$.uuid").toString()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
                    assertThat(content.read("$.status").toString()).isNotBlank();
                    return null;
                });
    }

    @Test
    @PactTestFor(providerName = "designs-command", pactMethod = "deleteDesignWhenUsingCQRS", pactVersion = PactSpecVersion.V4)
    @MockServerConfig(providerName = "designs-command", port = "1120")
    public void shouldDeleteDesignWhenUsingCQRS(MockServer mockServer) throws IOException {
        Request.delete(mockServer.getUrl() + "/v1/designs/" + TestConstants.DESIGN_UUID_1)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer abcdef")
                .execute()
                .handleResponse(httpResponse -> {
                    assertThat(httpResponse.getCode()).isEqualTo(202);
                    final DocumentContext content = JsonPath.parse(httpResponse.getEntity().getContent());
                    assertThat(content.read("$.uuid").toString()).isEqualTo(TestConstants.DESIGN_UUID_1.toString());
                    assertThat(content.read("$.status").toString()).isNotBlank();
                    return null;
                });
    }
}
