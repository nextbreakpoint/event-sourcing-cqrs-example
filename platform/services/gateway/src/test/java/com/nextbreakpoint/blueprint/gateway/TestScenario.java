package com.nextbreakpoint.blueprint.gateway;

import com.jayway.restassured.config.RestAssuredConfig;
import com.nextbreakpoint.blueprint.common.test.Scenario;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import com.xebialabs.restito.server.StubServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestScenario {
  private Scenario scenario;

  public void before() throws IOException, InterruptedException {
    final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
    final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");

    final List<String> secretArgs = Arrays.asList(
            "--from-file",
            "keystore_server.jks=../../secrets/keystore_server.jks",
            "--from-file",
            "keystore_auth.jceks=../../secrets/keystore_auth.jceks",
            "--from-file",
            "keystore_client.jks=../../secrets/keystore_client.jks",
            "--from-file",
            "truststore_client.jks=../../secrets/truststore_client.jks",
            "--from-literal",
            "KEYSTORE_SECRET=secret"
    );

    final List<String> helmArgs = Arrays.asList(
            "--set=replicas=1",
            "--set=clientDomain=${serviceHost}",
            "--set=authApiUrl=http://${stubHost}:${stubPort}",
            "--set=accountsApiUrl=http://${stubHost}:${stubPort}",
            "--set=designsAggregateFetcherApiUrl=http://${stubHost}:${stubPort}",
            "--set=designsCommandProducerApiUrl=http://${stubHost}:${stubPort}",
            "--set=image.pullPolicy=Never",
            "--set=image.repository=integration/gateway",
            "--set=image.tag=${version}"
    );

    scenario = Scenario.builder()
            .withNamespace("integration")
            .withVersion(version)
            .withServiceName("gateway")
            .withBuildImage(buildImages)
            .withSecretArgs(secretArgs)
            .withHelmPath("../../helm")
            .withHelmArgs(helmArgs)
            .withMinikube()
            .withStubServer()
            .withConsul()
            .build();

    scenario.create();
  }

  public void after() throws IOException, InterruptedException {
    if (scenario != null) {
      scenario.destroy();
    }
  }

  public URL makeBaseURL(String path) throws MalformedURLException {
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
    return new URL("https://" + scenario.getServiceHost() + ":" + scenario.getServicePort() + "/" + normPath);
  }

  public RestAssuredConfig getRestAssuredConfig() {
    return scenario.getRestAssuredConfig();
  }

  public StubServer getStubServer() {
    return scenario.getStubServer();
  }

  public String getServiceHost() {
    return scenario.getServiceHost();
  }

  public String getServicePort() {
    return scenario.getServicePort();
  }

  public String getStubHost() {
    return scenario.getStubHost();
  }

  public String getStubPort() {
    return scenario.getStubPort();
  }

  public String getNamespace() {
    return scenario.getNamespace();
  }

  public String getVersion() {
    return scenario.getVersion();
  }

  public String makeAuthorization(String user, String role) {
    return VertxUtils.makeAuthorization(user, Collections.singletonList(role), "../../secrets/keystore_auth.jceks");
  }
}
