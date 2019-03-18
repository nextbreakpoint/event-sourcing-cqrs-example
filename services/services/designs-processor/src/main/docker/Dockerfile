FROM ${java.image}

COPY maven/ /maven
COPY entrypoint.sh /entrypoint.sh

RUN chmod u+x /entrypoint.sh

EXPOSE ${container.port}

ENV CONFIG_NAME ${config.name}
ENV SERVICE_JAR ${service.jar}

ENTRYPOINT ["sh"]
CMD ["/entrypoint.sh"]
