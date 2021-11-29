package com.nextbreakpoint.blueprint.designs;

import com.nextbreakpoint.blueprint.common.test.BuildUtils;
import com.nextbreakpoint.blueprint.common.test.ContainerUtils;
import com.nextbreakpoint.blueprint.common.test.TestUtils;
import com.nextbreakpoint.blueprint.common.test.VertxUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;

public class TestScenario {
  private static final int PORT = 30120;
  private final String serviceName = "designs-aggregate-fetcher";
  private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
  private final String nexusHost = TestUtils.getVariable("NEXUS_HOST", System.getProperty("nexus.host", "localhost"));
  private final String nexusPort = TestUtils.getVariable("NEXUS_PORT", System.getProperty("nexus.port", "8081"));
  private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");

  private Network network = Network.builder().driver("bridge").build();

  private GenericContainer cassandra = ContainerUtils.createCassandraContainer(network)
          .waitingFor(Wait.forLogMessage(".* Initializing test_designs_aggregate_fetcher.design.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

  private GenericContainer minio = ContainerUtils.createMinioContainer(network)
          .waitingFor(Wait.forLogMessage("Documentation: https://docs.min.io.*", 1).withStartupTimeout(Duration.ofSeconds(30)));

  private GenericContainer service = new GenericContainer(DockerImageName.parse("integration/" + serviceName + ":" + version))
          .withEnv("JAEGER_SERVICE_NAME", serviceName)
          .withEnv("KEYSTORE_SECRET", "secret")
          .withEnv("DATABASE_HOST", "cassandra")
          .withEnv("DATABASE_KEYSPACE", TestConstants.DATABASE_KEYSPACE)
          .withEnv("DATABASE_USERNAME", "verticle")
          .withEnv("DATABASE_PASSWORD", "password")
          .withEnv("BUCKET_NAME", TestConstants.BUCKET)
          .withEnv("MINIO_HOST", "minio")
          .withEnv("MINIO_PORT", "9000")
          .withEnv("AWS_ACCESS_KEY_ID", "admin")
          .withEnv("AWS_SECRET_ACCESS_KEY", "password")
          .withFileSystemBind("../../secrets/keystore_server.jks", "/secrets/keystore_server.jks", BindMode.READ_ONLY)
          .withFileSystemBind("../../secrets/keystore_auth.jceks", "/secrets/keystore_auth.jceks", BindMode.READ_ONLY)
          .withFileSystemBind("config/integration.json", "/etc/config.json", BindMode.READ_ONLY)
          .withExposedPorts(PORT)
          .withNetwork(network)
          .withNetworkAliases(serviceName)
          .dependsOn(cassandra, minio)
          .waitingFor(Wait.forLogMessage(".* Service listening on port " + PORT + ".*", 1).withStartupTimeout(Duration.ofSeconds(20)));

  public void before() {
    if (buildImages) {
      BuildUtils.of(nexusHost, nexusPort, serviceName, version).buildDockerImage();
    }

    cassandra.start();
    minio.start();
    service.start();
  }

  public void after() {
    service.stop();
    minio.stop();
    cassandra.stop();
  }

  public String getVersion() {
    return version;
  }

  public String getServiceHost() {
    return service.getHost();
  }

  public Integer getServicePort() {
    return service.getMappedPort(PORT);
  }

  public String getMinioHost() {
    return minio.getHost();
  }

  public Integer getMinioPort() {
    return minio.getMappedPort(9000);
  }

  public String getCassandraHost() {
    return cassandra.getHost();
  }

  public Integer getCassandraPort() {
    return cassandra.getMappedPort(9042);
  }

  public String makeAuthorization(String user, String role) {
    return VertxUtils.makeAuthorization(user, Collections.singletonList(role), "../../secrets/keystore_auth.jceks");
  }
}
