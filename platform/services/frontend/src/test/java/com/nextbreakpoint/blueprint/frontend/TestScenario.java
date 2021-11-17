package com.nextbreakpoint.blueprint.frontend;

import com.jayway.restassured.config.RestAssuredConfig;
import com.nextbreakpoint.blueprint.common.test.Scenario;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import com.xebialabs.restito.server.StubServer;
import io.vertx.rxjava.core.http.Cookie;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class TestScenario {
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");

  private Scenario scenario;

  public void before() throws IOException, InterruptedException {

    final List<String> secretArgs = Arrays.asList(
            "--from-file",
            "ca_cert.pem=../../secrets/ca_cert.pem",
            "--from-file",
            "server_cert.pem=../../secrets/server_cert.pem",
            "--from-file",
            "server_key.pem=../../secrets/server_key.pem"
    );

    final List<String> helmArgs = Arrays.asList(
            "--set=replicas=1",
            "--set=clientDomain=${serviceHost}",
            "--set=clientWebUrl=https://${serviceHost}:${servicePort}",
            "--set=clientAuthUrl=https://${serviceHost}:${servicePort}",
            "--set=image.pullPolicy=Never",
            "--set=image.repository=integration/frontend",
            "--set=image.tag=${version}"
    );

    scenario = Scenario.builder()
            .withNamespace("integration")
            .withVersion(version)
            .withTimestamp(System.currentTimeMillis())
            .withServiceName("frontend")
            .withBuildImage(buildImages)
            .withSecretArgs(secretArgs)
            .withHelmPath("../../helm")
            .withHelmArgs(helmArgs)
//            .withKubernetes()
//            .withMinikube()
            .withStubServer()
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
    return version;
  }

  public Cookie makeCookie(String user, String role) {
    return VertxUtils.makeCookie(user, Arrays.asList(role), scenario.getServiceHost(), "../../secrets/keystore_auth.jceks");
  }
}
