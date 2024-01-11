ARG version
FROM busybox:latest AS build
RUN wget -O /jmx-exporter.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.20.0/jmx_prometheus_javaagent-0.20.0.jar
FROM zookeeper:${version}
RUN apt update -y && apt-get install -y wget
RUN mkdir -p /opt/jmx-exporter && mkdir -p /etc/jmx-exporter
COPY --from=build /jmx-exporter.jar /opt/jmx-exporter/jmx-exporter.jar
COPY jmx-exporter.yml /etc/jmx-exporter/config.yml
COPY zookeeper-entrypoint.sh /zookeeper-entrypoint.sh
RUN chmod a+x /zookeeper-entrypoint.sh
ENTRYPOINT ["/zookeeper-entrypoint.sh"]
CMD ["zkServer.sh", "start-foreground"]
