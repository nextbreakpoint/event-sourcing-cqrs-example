package com.nextbreakpoint.blueprint.accounts;

import com.jayway.restassured.config.RestAssuredConfig;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.Scenario;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestScenario {
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");

  private Scenario scenario;

  public void before() throws IOException, InterruptedException {

    final List<String> secretArgs = Arrays.asList(
            "--from-file",
            "keystore_server.jks=../../secrets/keystore_server.jks",
            "--from-file",
            "keystore_auth.jceks=../../secrets/keystore_auth.jceks",
            "--from-literal",
            "KEYSTORE_SECRET=secret",
            "--from-literal",
            "DATABASE_USERNAME=verticle",
            "--from-literal",
            "DATABASE_PASSWORD=password"
    );

    final List<String> helmArgs = Arrays.asList(
            "--set=replicas=1",
            "--set=clientDomain=${serviceHost}",
            "--set=image.pullPolicy=Never",
            "--set=image.repository=integration/accounts",
            "--set=image.tag=${version}"
    );

    scenario = Scenario.builder()
            .withNamespace("integration")
            .withVersion(version)
            .withTimestamp(System.currentTimeMillis())
            .withServiceName("accounts")
            .withBuildImage(buildImages)
            .withSecretArgs(secretArgs)
            .withHelmPath("../../helm")
            .withHelmArgs(helmArgs)
//            .withKubernetes()
//            .withMinikube()
            .withMySQL()
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

  public String getServiceHost() {
    return scenario.getServiceHost();
  }

  public String getServicePort() {
    return scenario.getServicePort();
  }

  public String getNamespace() {
    return scenario.getNamespace();
  }

  public String getVersion() {
    return version;
  }

  public int executeMySQLCommand(String namespace, String database, String sql) throws IOException, InterruptedException {
    final List<String> command = Arrays.asList(
            "sh",
            "-c",
            "kubectl -n " + namespace + " exec -i $(kubectl -n integration get pods -l app=mysql -o json | jq -r '.items[0].metadata.name') -- mysql -u root --password=password -e \"" + sql + "\" " + database
    );
    return KubeUtils.executeCommand(command, true);
  }

  public String makeAuthorization(String user, String role) {
    return VertxUtils.makeAuthorization(user, Collections.singletonList(role), "../../secrets/keystore_auth.jceks");
  }

  public String getMySqlConnectionUrl(String db) {
    return String.format("jdbc:mysql://%s:%s/%s", scenario.getMySQLHost(), scenario.getMySQLPort(), db);
  }
}
