package com.nextbreakpoint.blueprint.accounts;

import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.config.RedirectConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.config.SSLConfig;
import com.nextbreakpoint.blueprint.common.test.KubeUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class TestScenario {
  private static final int PORT = 30110;
  private final String serviceName = "accounts";
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final String nexusHost = TestUtils.getVariable("NEXUS_HOST", System.getProperty("nexus.host", "localhost"));
  private final String nexusPort = TestUtils.getVariable("NEXUS_PORT", System.getProperty("nexus.port", "8081"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");

  private Network network = Network.builder().driver("bridge").build();

  private GenericContainer mysql = new GenericContainer(DockerImageName.parse("mysql:8.0.13"))
          .withEnv("MYSQL_ROOT_PASSWORD", TestConstants.DATABASE_PASSWORD)
          .withFileSystemBind("../../scripts/init.sql", "/docker-entrypoint-initdb.d/init.sql", BindMode.READ_ONLY)
          .withNetwork(network)
          .withNetworkAliases("mysql")
          .withExposedPorts(3306)
          .waitingFor(Wait.forLogMessage(".* socket: '/var/run/mysqld/mysqld.sock'  port: 3306.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

  private GenericContainer service = new GenericContainer(DockerImageName.parse("integration/" + serviceName + ":" + version))
          .withEnv("KEYSTORE_SECRET", "secret")
          .withEnv("DATABASE_HOST", "mysql")
          .withEnv("DATABASE_NAME", TestConstants.DATABASE_NAME)
          .withEnv("DATABASE_USERNAME", "verticle")
          .withEnv("DATABASE_PASSWORD", "password")
          .withEnv("JAEGER_SERVICE_NAME", serviceName)
          .withFileSystemBind("../../secrets/keystore_server.jks", "/secrets/keystore_server.jks", BindMode.READ_ONLY)
          .withFileSystemBind("../../secrets/keystore_auth.jceks", "/secrets/keystore_auth.jceks", BindMode.READ_ONLY)
          .withFileSystemBind("config/integration.json", "/etc/config.json", BindMode.READ_ONLY)
          .withExposedPorts(PORT)
          .withNetwork(network)
          .withNetworkAliases(serviceName)
          .dependsOn(mysql)
          .waitingFor(Wait.forLogMessage(".* Service listening on port " + PORT + ".*", 1).withStartupTimeout(Duration.ofSeconds(20)));

  public void before() {
    if (buildImages) {
      buildDockerImages();
    }

    mysql.start();
    service.start();
  }

  public void after() {
    service.stop();
    mysql.stop();
  }

  public URL makeBaseURL(String path) throws MalformedURLException {
    final String normPath = path.startsWith("/") ? path.substring(1) : path;
    return new URL("https://" + service.getHost() + ":" + service.getMappedPort(PORT) + "/" + normPath);
  }

  public RestAssuredConfig getRestAssuredConfig() {
    final SSLConfig sslConfig = new SSLConfig().allowAllHostnames().and().relaxedHTTPSValidation();
    final RedirectConfig redirectConfig = new RedirectConfig().followRedirects(false);
    final LogConfig logConfig = new LogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
    return RestAssuredConfig.newConfig().redirect(redirectConfig).sslConfig(sslConfig).logConfig(logConfig);
  }

  public String getServiceHost() {
    return service.getHost();
  }

  public Integer getServicePort() {
    return service.getMappedPort(PORT);
  }

  public String getVersion() {
    return version;
  }

  public String makeAuthorization(String user, String role) {
    return VertxUtils.makeAuthorization(user, Collections.singletonList(role), "../../secrets/keystore_auth.jceks");
  }

  public String getMySqlConnectionUrl(String db) {
    return String.format("jdbc:mysql://%s:%s/%s", mysql.getHost(), mysql.getMappedPort(3306), db);
  }

  private void buildDockerImages() {
    System.out.println("Building image...");
    List<String> args = List.of(
            "--build-arg",
            "nexus_host=" + nexusHost,
            "--build-arg",
            "nexus_port=" + nexusPort
    );
    try {
      if (KubeUtils.buildDockerImage(".", "integration/" + serviceName + ":" + version, args) != 0) {
        throw new RuntimeException("Can't build image");
      }
    } catch (IOException e) {
      throw new RuntimeException("Can't build image");
    } catch (InterruptedException e) {
      throw new RuntimeException("Can't build image", e);
    }
    System.out.println("Image created");
  }
}
