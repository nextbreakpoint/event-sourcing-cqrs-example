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
    private static final int HTTP_PORT = 30123;
    private static final int DEBUG_PORT = 33123;
    private final String serviceName = "designs-notify";
    private final String version = TestUtils.getVariable("BUILD_VERSION", System.getProperty("build.version", "0"));
    private final String nexusHost = TestUtils.getVariable("NEXUS_HOST", System.getProperty("nexus.host", "localhost"));
    private final String nexusPort = TestUtils.getVariable("NEXUS_PORT", System.getProperty("nexus.port", "8081"));
    private final boolean buildImages = TestUtils.getVariable("BUILD_IMAGES", System.getProperty("build.images", "false")).equals("true");
    private final boolean useContainers = TestUtils.getVariable("USE_CONTAINERS", System.getProperty("use.containers", "true")).equals("true");
    private final String dockerHost = TestUtils.getVariable("DOCKER_HOST", System.getProperty("docker.host", "host.docker.internal"));

    private Network network = Network.builder().driver("bridge").build();

    private GenericContainer consul = ContainerUtils.createConsulContainer(network, "../../scripts/consul.json")
            .waitingFor(Wait.forLogMessage(".* agent: Synced service: service=designs-sse.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

    private GenericContainer zookeeper = ContainerUtils.createZookeeperContainer(network)
            .waitingFor(Wait.forLogMessage(".* binding to port /0.0.0.0:2181.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

    private GenericContainer kafka = ContainerUtils.createKafkaContainer(network)
            .dependsOn(zookeeper)
            .waitingFor(Wait.forLogMessage(".* started \\(kafka.server.KafkaServer\\).*", 1).withStartupTimeout(Duration.ofSeconds(90)));

    private GenericContainer service = new GenericContainer(DockerImageName.parse("integration/" + serviceName + ":" + version))
            .withEnv("DEBUG_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:" + DEBUG_PORT)
            .withEnv("JAEGER_SERVICE_NAME", serviceName)
            .withEnv("KEYSTORE_SECRET", "secret")
            .withEnv("CONSUL_HOST", "consul")
            .withEnv("KAFKA_HOST", "kafka")
            .withEnv("KAFKA_PORT", "9092")
            .withEnv("EVENTS_TOPIC", TestConstants.EVENTS_TOPIC_NAME)
            .withFileSystemBind("../../secrets/keystore_server.jks", "/secrets/keystore_server.jks", BindMode.READ_ONLY)
            .withFileSystemBind("../../secrets/keystore_auth.jceks", "/secrets/keystore_auth.jceks", BindMode.READ_ONLY)
            .withFileSystemBind("config/integration.json", "/etc/config.json", BindMode.READ_ONLY)
            .withExposedPorts(HTTP_PORT, DEBUG_PORT)
            .withNetwork(network)
            .withNetworkAliases(serviceName)
            .dependsOn(consul, zookeeper, kafka)
            .waitingFor(Wait.forLogMessage(".* Service listening on port " + HTTP_PORT + ".*", 1).withStartupTimeout(Duration.ofSeconds(20)));

    public void before() {
        if (buildImages) {
            BuildUtils.of(nexusHost, nexusPort, serviceName, version).buildDockerImage();
        }

        if (useContainers) {
            consul.start();
            zookeeper.start();
            kafka.start();
            service.start();

            System.out.println("Debug port: " + service.getMappedPort(DEBUG_PORT));
            System.out.println("Http port: " + service.getMappedPort(HTTP_PORT));
        }
    }

    public void after() {
        if (useContainers) {
            service.stop();
            kafka.stop();
            zookeeper.stop();
            consul.stop();
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
        return getPort(service, HTTP_PORT);
    }

    public String getKafkaHost() {
        return getHost(kafka);
    }

    public Integer getKafkaPort() {
        return getPort(kafka, 9093);
    }
}