ARG version
FROM busybox:latest AS build
RUN wget -O /jmx-exporter.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.20.0/jmx_prometheus_javaagent-0.20.0.jar
FROM confluentinc/cp-kafka:${version}
USER root
RUN mkdir -p /opt/jmx-exporter && mkdir -p /etc/jmx-exporter
COPY --from=build /jmx-exporter.jar /opt/jmx-exporter/jmx-exporter.jar
COPY jmx-exporter.yml /etc/jmx-exporter/config.yml
COPY kafka-entrypoint.sh /kafka-entrypoint.sh
RUN chmod a+x /kafka-entrypoint.sh
USER appuser
ENTRYPOINT ["/kafka-entrypoint.sh"]
CMD ["/etc/confluent/docker/run"]
