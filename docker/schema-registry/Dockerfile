ARG version
FROM busybox:latest AS build
RUN wget -O /jmx-exporter.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.20.0/jmx_prometheus_javaagent-0.20.0.jar
FROM confluentinc/cp-schema-registry:${version}
USER root
RUN mkdir -p /opt/jmx-exporter && mkdir -p /etc/jmx-exporter
COPY --from=build /jmx-exporter.jar /opt/jmx-exporter/jmx-exporter.jar
COPY jmx-exporter.yml /etc/jmx-exporter/config.yml
COPY schema-registry-entrypoint.sh /schema-registry-entrypoint.sh
RUN chmod a+x /schema-registry-entrypoint.sh
USER appuser
ENTRYPOINT ["/schema-registry-entrypoint.sh"]
CMD ["/etc/confluent/docker/run"]
