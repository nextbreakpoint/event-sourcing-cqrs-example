package com.nextbreakpoint.blueprint.common.test;

import java.io.IOException;
import java.util.List;

public class BuildUtils {
    private final String nexusHost;
    private final String nexusPort;
    private final String serviceName;
    private final String version;

    private BuildUtils(String nexusHost, String nexusPort, String serviceName, String version) {
        this.nexusHost = nexusHost;
        this.nexusPort = nexusPort;
        this.serviceName = serviceName;
        this.version = version;
    }

    public static BuildUtils of(String nexusHost, String nexusPort, String serviceName, String version) {
        return new BuildUtils(nexusHost, nexusPort, serviceName, version);
    }

    public void buildDockerImage() {
        System.out.println("Building image...");
        List<String> args = List.of(
                "--build-arg",
                "maven_args=\"-Dnexus.host=" + nexusHost + " -Dnexus.port=" + nexusPort + "\""
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
