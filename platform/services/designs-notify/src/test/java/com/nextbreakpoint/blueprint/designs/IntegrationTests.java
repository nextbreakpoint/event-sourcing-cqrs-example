package com.nextbreakpoint.blueprint.designs;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.nextbreakpoint.blueprint.common.core.OutputMessage;
import com.nextbreakpoint.blueprint.common.events.DesignAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.TileAggregateUpdateCompleted;
import com.nextbreakpoint.blueprint.common.events.mappers.DesignAggregateUpdateCompletedOutputMapper;
import com.nextbreakpoint.blueprint.common.events.mappers.TileAggregateUpdateCompletedOutputMapper;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

@Tag("slow")
@Tag("integration")
@DisplayName("Verify behaviour of designs-notify service")
public class IntegrationTests {
    private static TestCases testCases = new TestCases();

    @BeforeAll
    public static void before() {
        testCases.before();
    }

    @AfterAll
    public static void after() {
        testCases.after();
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingADesignAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted1 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0, TestConstants.JSON_1, TestConstants.CHECKSUM_1, TestConstants.LEVELS, "CREATED");

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted2 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0, TestConstants.JSON_2, TestConstants.CHECKSUM_2, TestConstants.LEVELS, "UPDATED");

        final OutputMessage designAggregateUpdateCompletedMessage1 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a DesignAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnDesignAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted1 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0, TestConstants.JSON_1, TestConstants.CHECKSUM_1, TestConstants.LEVELS, "CREATED");

        final DesignAggregateUpdateCompleted designAggregateUpdateCompleted2 = new DesignAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0, TestConstants.JSON_2, TestConstants.CHECKSUM_2, TestConstants.LEVELS, "UPDATED");

        final OutputMessage designAggregateUpdateCompletedMessage1 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new DesignAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of all resources after receiving a TileAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfAllResourcesWhenReceivingATileAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted1 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0);

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted2 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designAggregateUpdateCompletedMessage1 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfAllResourcesWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

    @Test
    @DisplayName("Should notify watchers of single resource after receiving a TileAggregateUpdateCompleted event")
    public void shouldNotifyWatchersOfSingleResourceWhenReceivingAnTileAggregateUpdateCompletedEvent() {
        final UUID designId1 = UUID.randomUUID();

        final UUID designId2 = UUID.randomUUID();

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted1 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId1, 0);

        final TileAggregateUpdateCompleted designAggregateUpdateCompleted2 = new TileAggregateUpdateCompleted(Uuids.timeBased(), designId2, 0);

        final OutputMessage designAggregateUpdateCompletedMessage1 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted1);

        final OutputMessage designAggregateUpdateCompletedMessage2 = new TileAggregateUpdateCompletedOutputMapper(TestConstants.MESSAGE_SOURCE).transform(designAggregateUpdateCompleted2);

        testCases.shouldNotifyWatchersOfSingleResourceWhenReceivingAnEvent(List.of(designAggregateUpdateCompletedMessage1, designAggregateUpdateCompletedMessage2));
    }

//    private static final String SCRIPT = "fractal {\norbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\nloop [0, 200] (mod2(x) > 40) {\nx = x * x + w;\n}\n}\ncolor [#FF000000] {\npalette gradient {\n[#FFFFFFFF > #FF000000, 100];\n[#FF000000 > #FFFFFFFF, 100];\n}\ninit {\nm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n}\nrule (n > 0) [1] {\ngradient[m - 1]\n}\n}\n}\n";
//    private static final String METADATA = "{\"translation\":{\"x\":0.0,\"y\":0.0,\"z\":1.0,\"w\":0.0},\"rotation\":{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"w\":0.0},\"scale\":{\"x\":1.0,\"y\":1.0,\"z\":1.0,\"w\":1.0},\"point\":{\"x\":0.0,\"y\":0.0},\"julia\":false,\"options\":{\"showPreview\":false,\"showTraps\":false,\"showOrbit\":false,\"showPoint\":false,\"previewOrigin\":{\"x\":0.0,\"y\":0.0},\"previewSize\":{\"x\":0.25,\"y\":0.25}}}";
//    private static final String MANIFEST = "{\"pluginId\":\"Mandelbrot\"}";
//
//    @Test
//    @DisplayName("should forward a GET request for a design")
//    public void shouldForwardAGETRequestForADesign() throws MalformedURLException {
//        final UUID designUuid = UUID.randomUUID();
//        final UUID accountUuid = UUID.randomUUID();
//
//        final Date date = new Date();
//
//        final String json = new JsonObject()
//                .put("manifest", MANIFEST)
//                .put("metadata", METADATA)
//                .put("script", SCRIPT)
//                .encode();
//
//        final String content = new JsonObject()
//                .put("uuid", designUuid.toString())
//                .put("json", json)
//                .put("checksum", "1")
//                .put("modified", DateTimeFormatter.ISO_INSTANT.format(date.toInstant()))
//                .encode();
//
//        whenHttp(scenario.getStubServer())
//                .match(get("/designs/" + designUuid), withHeader("accept", "application/json"))
//                .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent(content));
//
//        final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.GUEST);
//
//        given().config(scenario.getRestAssuredConfig())
//                .with().header("authorization", authorization)
//                .with().header("accept", "application/json")
//                .when().get(scenario.makeBaseURL("/designs/" + designUuid))
//                .then().assertThat().statusCode(200)
//                .and().contentType(ContentType.JSON);
//    }
//
//    // TODO add tests for other methods
//
//    @Test
//    @DisplayName("should forward a GET request for an account")
//    public void shouldForwardAGETRequestForAnAccount() throws MalformedURLException {
//        final UUID accountUuid = UUID.randomUUID();
//
//        whenHttp(scenario.getStubServer())
//                .match(get("/accounts/" + accountUuid), withHeader("accept", "application/json"))
//                .then(status(HttpStatus.OK_200), contentType("application/json"), stringContent("{\"name\":\"test\",\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));
//
//        final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.GUEST);
//
//        given().config(scenario.getRestAssuredConfig())
//                .with().header("authorization", authorization)
//                .with().header("accept", "application/json")
//                .when().get(scenario.makeBaseURL("/accounts/" + accountUuid))
//                .then().assertThat().statusCode(200)
//                .and().contentType(ContentType.JSON);
//    }
//
//    @Test
//    @DisplayName("should forward a POST request for an account")
//    public void shouldForwardAPOSTRequestForAnAccount() throws MalformedURLException {
//        final UUID accountUuid = UUID.randomUUID();
//
//        whenHttp(scenario.getStubServer())
//                .match(post("/accounts"), withHeader("authorization"), withHeader("content-type"), withPostBody())
//                .then(status(HttpStatus.CREATED_201), contentType("application/json"), stringContent("{\"role\":\"guest\",\"uuid\":\"" + accountUuid + "\"}"));
//
//        final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.ADMIN);
//
//        final Map<String, Object> account = createAccountData("user@localhost", "guest");
//
//        given().config(scenario.getRestAssuredConfig())
//                .with().header("authorization", authorization)
//                .with().header("content-type", "application/json")
//                .with().body(account)
//                .when().post(scenario.makeBaseURL("/accounts"))
//                .then().assertThat().statusCode(201)
//                .and().contentType(ContentType.JSON)
//                .and().body("uuid", equalTo(accountUuid.toString()));
//    }
//
//    @Test
//    @DisplayName("should return a Location when sending a watch request for a design")
//    public void shouldReturnALocationWhenSendingAWatchRequestForADesign() throws MalformedURLException {
//        final UUID designUuid = UUID.randomUUID();
//        final UUID accountUuid = UUID.randomUUID();
//
//        final String authorization = scenario.makeAuthorization(accountUuid.toString(), Authority.GUEST);
//
//        final String location = given().config(scenario.getRestAssuredConfig())
//                .with().header("authorization", authorization)
//                .with().header("accept", "application/json")
//                .when().get(scenario.makeBaseURL("/watch/designs/0/" + designUuid))
//                .then().assertThat().statusCode(200)
//                .and().header("location", startsWith("https://localhost:8080"))
//                .and().header("location", endsWith("/sse/designs/0/" + designUuid))
//                .extract().header("location");
//
//        System.out.println(location);
//    }
//
//    private static Map<String, Object> createAccountData(String email, String role) {
//        final Map<String, Object> data = new HashMap<>();
//        data.put("email", email);
//        data.put("name", "test");
//        data.put("role", role);
//        return data;
//    }
}
