package com.nextbreakpoint.blueprint.common.test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class KubeUtils {
    private KubeUtils() {}

    public static int createNamespace(String namespace) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "kubectl",
                "create",
                "namespace",
                namespace
        );
        return executeCommand(command, true);
    }

    public static int deleteNamespace(String namespace) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "kubectl",
                "delete",
                "namespace",
                namespace
        );
        return executeCommand(command, true);
    }

    public static int installHelmChart(String namespace, String name, String path, List<String> args, boolean print) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>(Arrays.asList(
                "helm",
                "install",
                "--namespace",
                namespace,
                name,
                path
        ));
        command.addAll(args);
        return executeCommand(command, print);
    }

    public static int upgradeHelmChart(String namespace, String name, String path, List<String> args, boolean print) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>(Arrays.asList(
                "helm",
                "upgrade",
                "--namespace",
                namespace,
                name,
                path
        ));
        command.addAll(args);
        return executeCommand(command, print);
    }

    public static int uninstallHelmChart(String namespace, String name) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "helm",
                "uninstall",
                "--namespace",
                namespace,
                name
        );
        return executeCommand(command, true);
    }

    public static int createSecret(String namespace, String name, List<String> args, boolean print) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>(Arrays.asList(
                "kubectl",
                "-n",
                namespace,
                "create",
                "secret",
                "generic",
                name
        ));
        command.addAll(args);
        return executeCommand(command, print);
    }

    public static int scaleDeployment(String namespace, String name, int replicas) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "kubectl",
                "-n",
                namespace,
                "scale",
                "deployment",
                name,
                "--replicas",
                String.valueOf(replicas)
        );
        return executeCommand(command,true);
    }

    public static int printLogs(String namespace, String name) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "kubectl -n " + namespace + " logs --tail=-1 -l app=" + name
        );
        return executeCommand(command, true);
    }

    public static String fetchLogs(String namespace, String name) throws IOException, InterruptedException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final List<String> command = Arrays.asList(
                    "kubectl",
                    "-n",
                    namespace,
                    "logs",
                    "--tail=-1",
                    "-l",
                    "app=" + name
            );
            if (executeCommand(command, true, baos) == 0) {
                return baos.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isPodRunning(String namespace, String name) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "kubectl -n " + namespace + " get pod --selector app=" + name + " -o json | jq --exit-status -r '.items[0].status.containerStatuses[] | select(.ready == true)' > /dev/null"
        );
        return executeCommand(command, true) == 0;
    }

    public static int describePods(String namespace) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "kubectl",
                "-n",
                namespace,
                "describe",
                "pod"
        );
        return executeCommand(command, true);
    }

    public static int exposeService(String namespace, String name, int port, int targetPort, final String externalIp) throws IOException, InterruptedException {
        if (externalIp != null) {
            final List<String> command = Arrays.asList(
                    "sh",
                    "-c",
                    "kubectl -n " + namespace + " expose service " + name + " --type=LoadBalancer --name=" + name + "-lb --port=" + port + " --target-port=" + targetPort + " --external-ip=" + externalIp
            );
            return KubeUtils.executeCommand(command, true);
        } else {
            final List<String> command = Arrays.asList(
                    "sh",
                    "-c",
                    "kubectl -n " + namespace + " expose service " + name + " --type=NodePort --name=" + name + "-lb --port=" + port + " --target-port=" + targetPort
            );
            return KubeUtils.executeCommand(command, true);
        }
    }

    public static int buildDockerImage(String path, String name, List<String> args) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "docker build -t " + name + " " + String.join(" ", args) + " " + path
        );
        return executeCommand(command, true);
    }

    public static int cleanDockerImages() throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "docker rmi $(docker images -f dangling=true -q)"
        );
        return executeCommand(command, true);
    }

    public static int buildDockerImageMinikube(String path, String name, List<String> args) throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "eval $(minikube docker-env) && docker build -t " + name + " " + String.join(" ", args) + " " + path
        );
        return executeCommand(command, true);
    }

    public static int cleanDockerImagesMinikube() throws IOException, InterruptedException {
        final List<String> command = Arrays.asList(
                "sh",
                "-c",
                "eval $(minikube docker-env) && docker rmi $(docker images -f dangling=true -q)"
        );
        return executeCommand(command, true);
    }

    public static String getMinikubeIp() throws IOException, InterruptedException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final List<String> command = Arrays.asList("minikube", "ip");
            if (executeCommand(command, false, baos) == 0) {
                String output = baos.toString();
                String[] lines = output.split("\n");
                System.out.println("Minikube ip: " + lines[0]);
                return lines[0];
            } else {
                throw new IOException("Can't get minikube ip address");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static int executeCommand(List<String> command, boolean print) throws IOException, InterruptedException {
        if (print) System.out.println(command.stream().collect(Collectors.joining(" ", "# ", "")));
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        final Map<String, String> environment = processBuilder.environment();
        environment.put("KUBECONFIG", TestUtils.getVariable("KUBECONFIG", System.getProperty("user.home") + "/.kube/config"));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        final Process process = processBuilder.start();
        int exitValue = process.waitFor();
        return exitValue;
    }

    public static int executeCommand(List<String> command, boolean print, OutputStream outputStream) throws IOException, InterruptedException {
        if (print) System.out.println(command.stream().collect(Collectors.joining(" ", "# ", "")));
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        final Map<String, String> environment = processBuilder.environment();
        environment.put("KUBECONFIG", TestUtils.getVariable("KUBECONFIG", System.getProperty("user.home") + "/.kube/config"));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        final Process process = processBuilder.start();
        int exitValue = process.waitFor();
        TestUtils.copyBytes(process.getInputStream(), outputStream, 10240);
        return exitValue;
    }
}
