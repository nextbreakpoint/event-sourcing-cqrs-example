package com.nextbreakpoint.blueprint.authentication;

import com.nextbreakpoint.blueprint.common.test.BuildUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

public class TestScenario {
  private static final int HTTP_PORT = 30101;
  private static final int DEBUG_PORT = 33101;
  private final String serviceName = "authentication";
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final String nexusHost = TestUtils.getVariable("NEXUS_HOST", System.getProperty("nexus.host", "localhost"));
  private final String nexusPort = TestUtils.getVariable("NEXUS_PORT", System.getProperty("nexus.port", "8081"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");
  private final boolean useContainers = TestUtils.getVariable("USE_CONTAINERS", System.getProperty("use.containers", "true")).equals("true");
  private final boolean startPlatform = TestUtils.getVariable("START_PLATFORM", System.getProperty("start.platform", "false")).equals("true");
  private final String dockerHost = TestUtils.getVariable("DOCKER_HOST", System.getProperty("docker.host", "host.docker.internal"));
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  private Network network = Network.builder().driver("bridge").build();

  private GenericContainer<?> service = new GenericContainer<>(DockerImageName.parse("integration/" + serviceName + ":" + version))
          .withEnv("DEBUG_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:" + DEBUG_PORT)
          .withEnv("LOGGING_LEVEL", "DEBUG")
          .withEnv("JAEGER_SERVICE_NAME", serviceName)
          .withEnv("KEYSTORE_SECRET", "secret")
          .withEnv("GITHUB_ACCOUNT_ID", "admin@localhost")
          .withEnv("GITHUB_CLIENT_ID", "111")
          .withEnv("GITHUB_CLIENT_SECRET", "222")
          .withEnv("ACCOUNTS_URL", "http://" + dockerHost + ":39001")
          .withEnv("GITHUB_API_URL", "http://" + dockerHost + ":39002")
          .withEnv("GITHUB_OAUTH_URL", "http://" + dockerHost + ":39002")
          .withFileSystemBind("../../secrets/keystore_auth.jceks", "/secrets/keystore_auth.jceks", BindMode.READ_ONLY)
          .withFileSystemBind("config/integration.json", "/etc/config.json", BindMode.READ_ONLY)
          .withExposedPorts(HTTP_PORT, DEBUG_PORT)
          .withNetwork(network)
          .withNetworkAliases(serviceName)
          .withLogConsumer(frame -> outputStream.writeBytes(Optional.ofNullable(frame.getBytes()).orElse(new byte[0])))
          .waitingFor(Wait.forLogMessage(".*\"Service listening on port " + HTTP_PORT + "\".*", 1).withStartupTimeout(Duration.ofSeconds(20)));

  public void before() {
    if (buildImages) {
      BuildUtils.of(nexusHost, nexusPort, serviceName, version).buildDockerImage();
    }

    if (useContainers) {
      service.start();

      System.out.println("Debug port: " + service.getMappedPort(DEBUG_PORT));
      System.out.println("Http port: " + service.getMappedPort(HTTP_PORT));
    } else {
      System.out.println("Don't start containers");
    }
  }

  public void after() {
    if (useContainers) {
      System.out.println("Service logs:");
      System.out.println(outputStream);

      service.stop();
    }
  }

  private String getHost(GenericContainer container) {
    return (useContainers && startPlatform) ? container.getHost() : "localhost";
  }

  private int getPort(GenericContainer container, int port) {
    return (useContainers && startPlatform) ? container.getMappedPort(port) : port;
  }

  public String getVersion() {
    return version;
  }

  public String getServiceHost() {
    return useContainers ? service.getHost() : getHost(service);
  }

  public Integer getServicePort() {
    return useContainers ? service.getMappedPort(HTTP_PORT) : HTTP_PORT;
  }

  public String makeAuthorization(String user, String role) {
    return VertxUtils.makeAuthorization(user, Collections.singletonList(role), "../../secrets/keystore_auth.jceks");
  }
}
