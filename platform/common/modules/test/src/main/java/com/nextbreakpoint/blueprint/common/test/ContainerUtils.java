package com.nextbreakpoint.blueprint.common.test;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class ContainerUtils {
    private ContainerUtils() {}

    public static GenericContainer createMySqlContainer(Network network, String password, String initScript) {
        return new GenericContainer(DockerImageName.parse("mysql:8.0.13"))
                .withEnv("MYSQL_ROOT_PASSWORD", password)
                .withFileSystemBind(initScript, "/docker-entrypoint-initdb.d/init.sql", BindMode.READ_ONLY)
                .withNetwork(network)
                .withNetworkAliases("mysql")
                .withExposedPorts(3306);
    }

    public static GenericContainer createZookeeperContainer(Network network) {
        return new GenericContainer(DockerImageName.parse("nextbreakpoint/zookeeper:3.6.2-1"))
                .withEnv("ZOO_MY_ID", "1")
                .withNetwork(network)
                .withNetworkAliases("zookeeper")
                .withExposedPorts(2181);
    }

    public static GenericContainer createKafkaContainer(Network network) {
        return new KafkaGenericContainer(DockerImageName.parse("nextbreakpoint/kafka:6.1.0-1"))
                .withEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT_EXTERNAL://localhost:9093,PLAINTEXT_INTERNAL://kafka:9092")
                .withEnv("KAFKA_LISTENERS", "PLAINTEXT_EXTERNAL://0.0.0.0:9093,PLAINTEXT_INTERNAL://0.0.0.0:9092")
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT_EXTERNAL:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT_INTERNAL")
                .withEnv("KAFKA_BROKER_ID", "1")
                .withEnv("KAFKA_BROKER_RACK", "rack1")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_NUM_PARTITIONS", "16")
                .withEnv("KAFKA_DELETE_TOPICS", "true")
                .withEnv("KAFKA_AUTO_CREATE_TOPICS", "true")
                .withEnv("KAFKA_LOG_RETENTION_HOURS", "24")
                .withEnv("KAFKA_TRANSACTION_MAX_TIMEOUT_MS", "60000")
                .withEnv("KAFKA_HEAP_OPTS", "-Xms1500M -Xmx1500M")
                .withEnv("CONFLUENT_SUPPORT_METRICS_ENABLE", "false")
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withExposedPorts(9092, 9093);
    }

    public static GenericContainer createCassandraContainer(Network network) {
        return new GenericContainer(DockerImageName.parse("nextbreakpoint/cassandra:3.11.10-1"))
                .withEnv("CASSANDRA_DC", "datacenter1")
                .withEnv("CASSANDRA_RACK", "rack1")
                .withEnv("JVM_OPTS", "-Xms2G -Xmx2G")
                .withFileSystemBind("../../scripts/init.cql", "/docker-entrypoint-initdb.d/init.cql", BindMode.READ_ONLY)
                .withNetwork(network)
                .withNetworkAliases("cassandra")
                .withExposedPorts(9042);
    }

    public static GenericContainer createMinioContainer(Network network) {
        return new GenericContainer(DockerImageName.parse("minio/minio:latest"))
                .withEnv("MINIO_ROOT_USER", "admin")
                .withEnv("MINIO_ROOT_PASSWORD", "password")
                .withNetwork(network)
                .withNetworkAliases("minio")
                .withExposedPorts(9000, 39090)
                .withCommand("server", "/var/data", "--console-address", ":39090");
    }

    public static GenericContainer createConsulContainer(Network network, String configFile) {
        return new GenericContainer(DockerImageName.parse("consul:1.9.4"))
                .withFileSystemBind(configFile, "/consul/config/config.json", BindMode.READ_ONLY)
                .withCommand("consul", "agent", "-data-dir=/consul/data", "-config-dir=/consul/config")
                .withNetwork(network)
                .withNetworkAliases("consul")
                .withExposedPorts(8300, 8301, 8302, 8400, 8600);
    }

    private static class KafkaGenericContainer extends GenericContainer {
        public KafkaGenericContainer(DockerImageName image) {
            super(image);
            addFixedExposedPort(9093, 9093);
        }
    }
}