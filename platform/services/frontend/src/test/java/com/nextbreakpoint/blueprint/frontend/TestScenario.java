package com.nextbreakpoint.blueprint.frontend;

import com.nextbreakpoint.blueprint.common.test.BuildUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class TestScenario {
  private static final int PORT = 30400;
  private final String serviceName = "frontend";
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final String nexusHost = TestUtils.getVariable("NEXUS_HOST", System.getProperty("nexus.host", "localhost"));
  private final String nexusPort = TestUtils.getVariable("NEXUS_PORT", System.getProperty("nexus.port", "8081"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");
  private final boolean useContainers = TestUtils.getVariable("USE_CONTAINERS", System.getProperty("use.containers", "true")).equals("true");
  private final String dockerHost = TestUtils.getVariable("DOCKER_HOST", System.getProperty("docker.host", "host.docker.internal"));

  private Network network = Network.builder().driver("bridge").build();

  private GenericContainer service = new GenericContainer(DockerImageName.parse("integration/" + serviceName + ":" + version))
          .withEnv("JAEGER_SERVICE_NAME", serviceName)
          .withEnv("SECRETS_PATH", "/secrets")
          .withEnv("CONFIG_PATH", "/etc/config.json")
          .withEnv("PORT", String.valueOf(PORT))
          .withFileSystemBind("../../secrets/ca_cert.pem", "/secrets/ca_cert.pem", BindMode.READ_ONLY)
          .withFileSystemBind("../../secrets/server_cert.pem", "/secrets/server_cert.pem", BindMode.READ_ONLY)
          .withFileSystemBind("../../secrets/server_key.pem", "/secrets/server_key.pem", BindMode.READ_ONLY)
          .withFileSystemBind("config/integration.json", "/etc/config.json", BindMode.READ_ONLY)
          .withExposedPorts(PORT)
          .withNetwork(network)
          .withNetworkAliases(serviceName)
          .waitingFor(Wait.forLogMessage("Service listening on port " + PORT + ".*", 1).withStartupTimeout(Duration.ofSeconds(20)));

  public void before() {
    if (buildImages) {
      BuildUtils.of(nexusHost, nexusPort, serviceName, version).buildDockerImage();
    }

    if (useContainers) {
      service.start();
    }
  }

  public void after() {
    if (useContainers) {
      service.stop();
    }
  }

  private String getHost(GenericContainer container) {
    return useContainers ? container.getHost() : "localhost";
  }

  private int getPort(GenericContainer container, int port) {
    return useContainers ? container.getMappedPort(port) : port;
  }

  public String getVersion() {
    return version;
  }

  public String getServiceHost() {
    return getHost(service);
  }

  public Integer getServicePort() {
    return getPort(service, PORT);
  }
}
