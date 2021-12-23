package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.test.BuildUtils;
import com.nextbreakpoint.blueprint.common.test.ContainerUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class TestScenario {
  private static final int HTTP_PORT = 30124;
  private static final int DEBUG_PORT = 33124;
  private final String serviceName = "designs-render";
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final String nexusHost = TestUtils.getVariable("NEXUS_HOST", System.getProperty("nexus.host", "localhost"));
  private final String nexusPort = TestUtils.getVariable("NEXUS_PORT", System.getProperty("nexus.port", "8081"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");

  private Network network = Network.builder().driver("bridge").build();

  private GenericContainer zookeeper = ContainerUtils.createZookeeperContainer(network)
          .waitingFor(Wait.forLogMessage(".* binding to port /0.0.0.0:2181.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

  private GenericContainer kafka = ContainerUtils.createKafkaContainer(network)
          .dependsOn(zookeeper)
          .waitingFor(Wait.forLogMessage(".* started \\(kafka.server.KafkaServer\\).*", 1).withStartupTimeout(Duration.ofSeconds(90)));

  private GenericContainer minio = ContainerUtils.createMinioContainer(network)
          .waitingFor(Wait.forLogMessage("Documentation: https://docs.min.io.*", 1).withStartupTimeout(Duration.ofSeconds(30)));

  private GenericContainer service = new GenericContainer(DockerImageName.parse("integration/" + serviceName + ":" + version))
          .withEnv("DEBUG_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:" + DEBUG_PORT)
          .withEnv("JAEGER_SERVICE_NAME", serviceName)
          .withEnv("KEYSTORE_SECRET", "secret")
          .withEnv("CONSUL_HOST", "consul")
          .withEnv("KAFKA_HOST", "kafka")
          .withEnv("KAFKA_PORT", "9092")
          .withEnv("EVENTS_TOPIC", TestConstants.EVENTS_TOPIC_NAME)
          .withEnv("RENDER_TOPIC", TestConstants.RENDER_TOPIC_NAME)
          .withEnv("BUCKET_NAME", TestConstants.BUCKET)
          .withEnv("MINIO_HOST", "minio")
          .withEnv("MINIO_PORT", "9000")
          .withEnv("AWS_ACCESS_KEY_ID", "admin")
          .withEnv("AWS_SECRET_ACCESS_KEY", "password")
          .withFileSystemBind("../../secrets/keystore_server.jks", "/secrets/keystore_server.jks", BindMode.READ_ONLY)
          .withFileSystemBind("../../secrets/keystore_auth.jceks", "/secrets/keystore_auth.jceks", BindMode.READ_ONLY)
          .withFileSystemBind("config/integration.json", "/etc/config.json", BindMode.READ_ONLY)
          .withExposedPorts(HTTP_PORT, DEBUG_PORT)
          .withNetwork(network)
          .withNetworkAliases(serviceName)
          .dependsOn(zookeeper, kafka, minio)
          .waitingFor(Wait.forLogMessage(".* Service listening on port " + HTTP_PORT + ".*", 1).withStartupTimeout(Duration.ofSeconds(20)));

  public void before() {
    if (buildImages) {
      BuildUtils.of(nexusHost, nexusPort, serviceName, version).buildDockerImage();
    }

    zookeeper.start();
    kafka.start();
    minio.start();
    service.start();

    System.out.println("Debug port: " + service.getMappedPort(DEBUG_PORT));
    System.out.println("Http port: " + service.getMappedPort(HTTP_PORT));
  }

  public void after() {
    service.stop();
    minio.stop();
    kafka.stop();
    zookeeper.stop();
  }

  public String getVersion() {
    return version;
  }

  public String getServiceHost() {
    return service.getHost();
  }

  public Integer getServicePort() {
    return service.getMappedPort(HTTP_PORT);
  }

  public String getKafkaHost() {
    return kafka.getHost();
  }

  public Integer getKafkaPort() {
    return kafka.getMappedPort(9093);
  }

  public String getMinioHost() {
    return minio.getHost();
  }

  public Integer getMinioPort() {
    return minio.getMappedPort(9000);
  }
}
